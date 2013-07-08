package akka.router

import akka.actor.ActorLogging
import akka.actor.Actor
import org.mashupbots.socko.events.WebSocketFrameEvent
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import akka.actor.ActorRef
import akka.routing.DefaultResizer
import akka.routing.RoundRobinRouter
import akka.actor.Props
import akka.router.routingActor.forwardDecoderActor
import akka.router.adminActors.workerManagerActor
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration._
import java.io.File
import scala.xml.XML


sealed trait dipatcherEvents
case class dipatcherMessage(actorToSendMessage: String, dataMessage: String, webSocket : WebSocketFrameEvent) extends dipatcherEvents;

case class jsonRouteDispatcherMessage(dispatcher: String, data: String);

class routerDispatcherActor(clientLogActor: ActorRef, adminActor : ActorRef) extends Actor with ActorLogging  {
  
  implicit val formats = DefaultFormats; // Brings in default date formats etc for JSON Lift
  
  //creating actors
  
  var forwardDcodeActor : ActorRef = null;
  var workerManagementActor : ActorRef = null;
  
  var actorLower : Int = 1;
  var actorUpper : Int = 10;
  
  override def preStart() {
    log.info("Starting routerDispatcherActor (routerDispatcherActor under masterRouterActor) instance hashcode # {}", this.hashCode());  
    //val resizer = new DefaultResizer(lowerBound = 1,upperBound = 10);
    log.info("Initialising routerDispatcherActor child actors: forwardDecoderActor & workerManagerActor. Hashcode # {}", this.hashCode());
    
    actorConfig();
    val resizer = new DefaultResizer(lowerBound = actorLower, upperBound = actorUpper);
    
    forwardDcodeActor = context.actorOf(Props(new forwardDecoderActor(clientLogActor)).withRouter(RoundRobinRouter(resizer = Some(resizer), supervisorStrategy = supervisorEscalator)), name = "forwardDecoderActor");
    workerManagementActor = context.actorOf(Props(new workerManagerActor(adminActor)).withRouter(RoundRobinRouter(resizer = Some(resizer), supervisorStrategy = supervisorEscalator)), name = "workerManagerActor");
    
  }
  
  /* Overriding postRestart to disable the call to preStart()
   * after restarts*/
  override def postRestart(reason: Throwable): Unit = {
    //Reassign the ActorRefs, since the after the ActorRefs are now null
    forwardDcodeActor = context.child("forwardDecoderActor").get;
    workerManagementActor = context.child("workerManagerActor").get;
    log.info("Restarting routerDispatcherActor (routerDispatcherActor under masterRouterActor) instance hashcode # {}", this.hashCode());
  } 
  
  /* The default implementation of preRestart() stops all the children
   * of the actor. To opt-out from stopping the children, we
   * have to override preRestart()*/
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    // Keep the call to postStop(), but no stopping of children
    postStop();
    log.info("Stopping routerDispatcherActor for restart (routerDispatcherActor under masterRouterActor) instance hashcode # {}", this.hashCode());
    }
  
  
  val supervisorEscalator = OneForOneStrategy(maxNrOfRetries = 100, withinTimeRange = 10 seconds) {
    //if a child actor fails restart it
  	case _: Exception => Restart
  }
  
  
  def receive = {
    case websocketEvent: WebSocketFrameEvent => {
      val eventMessage = websocketEvent.readText;
      val dispatchMessage = decodeRouteMessage(eventMessage, websocketEvent);
      
      val dispatchActor = dispatchMessage.actorToSendMessage;
      dispatchActor match{
        case "return" => {
          forwardDcodeActor ! dispatchMessage.dataMessage;
        }
        case "admin" =>{
          workerManagementActor ! dispatchMessage;          
        }
        
        case _=> log.info("Recied unknown message for routerDispactherActor: Did not dispatch message");
      }
      
    }
    
    case _=> log.info("unknown message")
  }
  
  def decodeRouteMessage(message : String, wsEvent: WebSocketFrameEvent): dipatcherMessage = {
    //println(message);
    val json = parse(message);
    val jsonData = json.extract[jsonRouteDispatcherMessage];
    
    val actorName = jsonData.dispatcher;
    val messageData = jsonData.data;
     
    val dMessage = new dipatcherMessage(actorName, messageData, wsEvent);
    return dMessage;
    //return null;
    
  }
  
    def actorConfig() : Unit = {
    var workingDirectory = new java.io.File(".").getCanonicalPath();
    var folderPath: String = workingDirectory + "/config/Actors/";
    var configFolder = new File(folderPath);
    
    if (configFolder.exists()){
      //store actor information
      var storageXML : scala.xml.Elem = readXMLFile(folderPath, "routing.xml");
      //readStoreContent(storageXML);
      
      if (storageXML != null){
              
        val actorLowerBound = (storageXML \"workerManagerActor"\ "lowerBound").text;
        val actorUpperBound = (storageXML \"workerManagerActor"\ "upperBound").text;
        actorLower = actorLowerBound.toInt;
        actorUpper = actorUpperBound.toInt;
        
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