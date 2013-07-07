package akka.voldemort.voldactors

import voldemort.client.DefaultStoreClient
import voldemort.versioning.Version
import voldemort.versioning.Versioned
import akka.actor.Actor
import akka.actor.Props
import akka.routing.RoundRobinRouter
import akka.routing.DefaultResizer
import akka.routing.FromConfig
import akka.voldemort.voldactors.actionactors.deleteActor
import akka.voldemort.voldactors.actionactors.getActor
import akka.actor.ActorLogging
import messages.voldemortMessage
import akka.voldemort.voldactors.actionactors.putActor
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.Await
import scala.util.{Success, Failure}
import scala.concurrent.TimeoutException
import akka.dispatch.Futures
import akka.dispatch.OnComplete
import scala.concurrent.ExecutionContext.Implicits.global
import org.mashupbots.socko.events.HttpRequestEvent
import org.mashupbots.socko.events.WebSocketFrameEvent
import messages.voldemortDirectMessage
import messages.voldemortMessage
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import configuration.storageManagement
import configuration.storage
import akka.actor.ActorRef
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import java.io.File
import scala.xml.XML


//voldemort actors internal messages
sealed trait voldEvents
//Delete any version of the given key which equal to or less than the current versions
case class delete(key: Object, clientStore: DefaultStoreClient[Object, Object]) extends voldEvents;
//Delete the specified version and any prior versions of the given key
case class deleteVersion(key: Object, version: Version, clientStore: DefaultStoreClient[Object, Object]) extends voldEvents;
//get value only
case class get(key: Object, clientStore: DefaultStoreClient[Object, Object]) extends voldEvents;
//Get the versioned value associated with the given key or the defaultValue if no value is associated with the key
case class getVersioned(key: Object, clientStore: DefaultStoreClient[Object, Object]) extends voldEvents;
//put value associated with specific key
case class put(key: Object, value: Object, clientStore: DefaultStoreClient[Object, Object]) extends voldEvents
//Put the given Versioned value into the store for the given key if the version is greater to or concurrent with existing values.
case class putVersion(key: Object, versioned: Versioned[Object] , clientStore: DefaultStoreClient[Object, Object]) extends voldEvents


case class jsonVoldMessage(operation: String, storeName: String ,key: String, value: String);

class voldCoordinator extends Actor with ActorLogging {
  
  //val voldDeleteActor: ActorRef = context.actorOf(Props[deleteActor], name = "deleteActor");
	implicit val formats = DefaultFormats; // Brings in default date formats etc for JSON Lift
   
  implicit var timeout = Timeout(5 seconds);
  var actorLower : Int = 2;
  var actorUpper : Int = 15;

  //creating actors
  
  var voldDeleteActor: ActorRef = null;
  var voldGetActor: ActorRef = null;
  var voldPutActor: ActorRef = null;
  
  //val voldDeleteActor: ActorRef = context.actorOf(Props[deleteActor].withRouter(FromConfig()), name = "deleteActor");
  //val voldGetActor: ActorRef = null;//context.actorOf(Props[getActor].withRouter(FromConfig()), name = "voldGetActor");
  //val voldPutActor: ActorRef = null;//context.actorOf(Props[putActor].withRouter(FromConfig()), name = "voldPutActor");
  
  

  def storeManager = storage.storageManager;
  
  override def preStart() {
    
    // Initialize children here
    log.info("Starting voldCoordinator (Voldemort Coordinator) instance hashcode # {}", this.hashCode());
    log.info("Configuring voldCoordinator (Voldemort Coordinator) child actors hashcode # {}", this.hashCode());
    
   
    actorConfig();
    val resizer = new DefaultResizer(lowerBound = actorLower, upperBound = actorUpper);
    voldDeleteActor = context.actorOf(Props[deleteActor].withRouter(RoundRobinRouter(resizer = Some(resizer), supervisorStrategy = supervisorEscalator)), name = "deleteActor");
    voldGetActor = context.actorOf(Props[getActor].withRouter(RoundRobinRouter(resizer = Some(resizer), supervisorStrategy = supervisorEscalator)), name = "getActor");
    voldPutActor = context.actorOf(Props[putActor].withRouter(RoundRobinRouter(resizer = Some(resizer), supervisorStrategy = supervisorEscalator)), name = "putActor");
    //voldPutActor = context.actorOf(Props[putActor].withRouter(FromConfig()), name = "voldPutActor");
    
  }
  
  /* Overriding postRestart to disable the call to preStart()
   * after restarts*/
  override def postRestart(reason: Throwable): Unit = {
    //voldGetActor = context.child("voldGet").get
    //Reassign the ActorRefs, since the after the ActorRefs are now null
    voldDeleteActor = context.child("deleteActor").get;
    voldGetActor = context.child("getActor").get;
    voldPutActor = context.child("putActor").get;
  } 
  
  /* The default implementation of preRestart() stops all the children
   * of the actor. To opt-out from stopping the children, we
   * have to override preRestart()*/
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    // Keep the call to postStop(), but no stopping of children
    postStop();
    }
  
  val supervisorEscalator = OneForOneStrategy(maxNrOfRetries = 100, withinTimeRange = 10 seconds) {
    //if a child actor fails restart it
  	case _: Exception => Restart
  }
  
  //override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 100, withinTimeRange = 10 seconds) {
    //if a child actor fails restart it
  //	case _: Exception => Restart
  //}
  
  
  def receive = {
    //internal system messages
    case voldMessage: voldemortDirectMessage => {
      val operation = voldMessage.op
      operation match {
        case "put" => voldPutActor ! new put(voldMessage.k, voldMessage.v, voldMessage.cStore);
        case "putver" => voldPutActor ! new putVersion(voldMessage.k, voldMessage.versionedValue, voldMessage.cStore);
        case "delete" => voldDeleteActor ! new delete(voldMessage.k, voldMessage.cStore);
        case "deletever" => voldDeleteActor ! new deleteVersion(voldMessage.k, voldMessage.vers, voldMessage.cStore);
        case "get" => {
          val msg = new get(voldMessage.k, voldMessage.cStore)
          voldGetActor ? msg onComplete {
            case Success(result: Versioned[Object]) => {
              //log.info("recieved value: " + result.getValue);
              val returnedValue = result.getValue.asInstanceOf[Object];
              val returnMessage = new voldemortDirectMessage("put",voldMessage.cStore, voldMessage.k, returnedValue,null,null,null);
              sender ! returnMessage;              
            }
            case Failure(e: TimeoutException) => {
              log.info("failed to retrieve value");
              val returnMessage = new voldemortDirectMessage("getfail",voldMessage.cStore, voldMessage.k, "timeout",null,null,null);
              sender ! returnMessage; 
            }
          }
        }
        case "getver" => {
          val msg = new getVersioned(voldMessage.k, voldMessage.cStore);
          voldGetActor ? msg onComplete {
            case Success(result) => {
              //log.info("recieved value: " + result);
              val returnedValue = result.asInstanceOf[Object];
              val returnMessage = new voldemortDirectMessage("put",voldMessage.cStore, voldMessage.k, returnedValue,null,null,null);
              sender ! returnMessage;
            }
            case Failure(e: TimeoutException) => {
              log.info("failed to retrieve value");
              val returnMessage = new voldemortDirectMessage("getfail",voldMessage.cStore, voldMessage.k, "timeout",null,null,null);
              sender ! returnMessage;
            }
          }
        }
        case _=> log.info("Recieved unknown direct operation message for voldemort operation: voldCoordinator");
      }
    }
    
    //external messages from websocket event
    case wsEvent: WebSocketFrameEvent => {
      val vMessageStringfied = wsEvent.readText;
      val voldMessage: voldemortMessage = decodeVoldemortMessage(vMessageStringfied);
      //check if the storage exists, if not reply with error message
      val storage: DefaultStoreClient[Object, Object] = storeManager.getStorage(voldMessage.cStoreName);
      /*println("\n \n \n \n");
      println(voldMessage.op);
      println(voldMessage.cStoreName);
      println(voldMessage.cStoreName.getClass().getName())
      println(storeManager.getStorage("test"));
      println(voldMessage.k);
      println(voldMessage.v);
      println("\n \n");
      println(storage);
      println("\n \n \n \n");*/
      //println("\n \n \n \n");
      //println("Socket channel");
      //println(wsEvent.channel.getId());
      //println("\n \n \n \n");
      
      val operation = voldMessage.op;
      operation match {
        case "put" => voldPutActor ! new put(voldMessage.k, voldMessage.v, storage);
        //case "putver" => voldPutActor ! new putVersion(voldMessage.k, voldMessage.versionedValue, voldMessage.cStore);
        case "delete" => voldDeleteActor ! new delete(voldMessage.k, storage);
        //case "deletever" => voldDeleteActor ! new deleteVersion(voldMessage.k, voldMessage.vers, voldMessage.cStore);
        case "get" => {          
          val msg = new get(voldMessage.k, storage)
          voldGetActor ? msg onComplete {
            case Success(result: Versioned[Object]) => {
              log.info("recieved value: " + result.getValue);
              val json = ("operation" -> "put")~("storeName" -> voldMessage.cStoreName)~("key" -> voldMessage.k.asInstanceOf[String])~("value" -> result.getValue.asInstanceOf[String]);
              val returnMessage: String = compact(render(json));
              wsEvent.writeText(returnMessage);  
              //println("message sent");
            }
            case Failure(e: TimeoutException) => {
              //log.info("failed to retrieve value");
              //send Failed Message
              val json = ("operation" -> "getfail")~("storeName" -> voldMessage.cStoreName)~("key" -> voldMessage.k.asInstanceOf[String])~("reason" -> "timeout");
              val returnMessage: String = compact(render(json));
              wsEvent.writeText(returnMessage);
              
            }
          }
        }
        /*case "getver" => {
          val msg = new getVersioned(voldMessage.k, voldMessage.cStore);
          voldGetActor ? msg onComplete {
            case Success(result) => log.info("recieved value: " + result);
            case Failure(e: TimeoutException) => log.info("failed to retrieve value");
          }
        }*/
        case _=> log.info("Recieved unknown http operation message for voldemort operation: voldCoordinator");
      }
      
      
      
    }
    
    
    
    case _=> log.info("unknown message")
  }
  
  def decodeVoldemortMessage(message : String): voldemortMessage = {
    val json = parse(message);      
    val jsonData = json.extract[jsonVoldMessage];
    
    val operation = jsonData.operation;
    val storeName = jsonData.storeName;
    val key : Object  = jsonData.key.asInstanceOf[Object];
    val value : Object = jsonData.value.asInstanceOf[Object];
    
    val vMessage = new voldemortMessage(operation, storeName, key, value);
    return vMessage;
    
  }
  
  def actorConfig() : Unit = {
    var workingDirectory = new java.io.File(".").getCanonicalPath();
    var folderPath: String = workingDirectory + "/config/Actors/";
    var configFolder = new File(folderPath);
    
    if (configFolder.exists()){
      //store actor information
      var storageXML : scala.xml.Elem = readXMLFile(folderPath, "storage.xml");
      //readStoreContent(storageXML);
      
      if (storageXML != null){
              
        val actorLowerBound = (storageXML \"actionActor"\ "lowerBound").text;
        val actorUpperBound = (storageXML \"actionActor"\ "upperBound").text;
        val actorTimeOut = (storageXML \"actionActor"\ "timeout").text;
        actorLower = actorLowerBound.toInt;
        actorUpper = actorUpperBound.toInt;
        val getActorTimeOutInt: Int = actorTimeOut.toInt;
        timeout = Timeout(getActorTimeOutInt seconds); 
        
      }
      
      
    } else {
      println(" \n \n" + "Error: The folder for storage sctors could not be found. Path given: " + configFolder + " \n \n");
      println("Will use generic settings")
     
    }
  }
  
  
  def readXMLFile(path: String, fileName: String): scala.xml.Elem ={
    
    var xmlFile = new File(path, fileName); 
    if (xmlFile.exists()){
      var xmlContent = XML.loadFile(xmlFile);
      println("Read XML file for: " + fileName);
      return xmlContent;
    } else {
      println("File not found for XML reading: " + fileName);
      return null;
    }
    
  }
  
}
