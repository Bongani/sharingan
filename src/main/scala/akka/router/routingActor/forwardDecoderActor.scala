package akka.router.routingActor

import akka.actor.Actor
import akka.actor.ActorLogging
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import akka.routing.DefaultResizer
import akka.routing.RoundRobinRouter
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration._
import java.io.File
import scala.xml.XML

sealed trait forwardEvents
case class forwardMessage(worker: String, operationData: String, clientChannel: Int) extends forwardEvents;


case class jsonForwardMessage(worker: String, operationData: String, clientChannel: Int);

class forwardDecoderActor(clientLogActor : ActorRef) extends Actor with ActorLogging{
  
  implicit val formats = DefaultFormats; // Brings in default date formats etc for JSON Lift
  
    
  //creating actors
  
  var forwardActor : ActorRef = null
  
  var actorLower : Int = 1;
  var actorUpper : Int = 5;
  
  
  override def preStart() {
    //val resizer = new DefaultResizer(lowerBound = 1,upperBound = 10);
    log.info("Starting forwardDecoderActor (forwardDecoderActor under routerDispatcherActor) instance hashcode # {}", this.hashCode());
    
    actorConfig();
    val resizer = new DefaultResizer(lowerBound = actorLower, upperBound = actorUpper);
    
    forwardActor = context.actorOf(Props(new forwardResultActor(clientLogActor)).withRouter(RoundRobinRouter(resizer = Some(resizer), supervisorStrategy = supervisorEscalator)), name = "forwardResultActor");
    
  }
  
  /* Overriding postRestart to disable the call to preStart()
   * after restarts*/
  override def postRestart(reason: Throwable): Unit = {
    //Reassign the ActorRefs, since the after the ActorRefs are now null
     forwardActor = context.child("forwardResultActor").get;
  } 
  
  /* The default implementation of preRestart() stops all the children
   * of the actor. To opt-out from stopping the children, we
   * have to override preRestart()*/
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    // Keep the call to postStop(), but no stopping of children
    postStop();
    log.info("Stopping forwardDecoderActor (forwardDecoderActor under routerDispatcherActor) instance hashcode # {}",this.hashCode());
    }
  
  
  val supervisorEscalator = OneForOneStrategy(maxNrOfRetries = 100, withinTimeRange = 10 seconds) {
    //if a child actor fails restart it
  	case _: Exception => Restart
  }
  
  
  
  def receive = {
    case message: String =>{
      val decodedForwardMessage = decodeJSONMessage(message);      
      forwardActor ! decodedForwardMessage;
    }
        
    case _=> log.info("unknown message")
  }
  
  def decodeJSONMessage(fMessage : String): forwardMessage = {
    val json = parse(fMessage);      
    val jsonData = json.extract[jsonForwardMessage];
    
    val workerName = jsonData.worker;
    val operateData = jsonData.operationData;
    val channel  = jsonData.clientChannel;
        
    val fwdMessage = new forwardMessage(workerName, operateData, channel);
    return fwdMessage;
    
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
              
        val actorLowerBound = (storageXML \"forwardResultActor"\ "lowerBound").text;
        val actorUpperBound = (storageXML \"forwardResultActor"\ "upperBound").text;
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