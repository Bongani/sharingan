package akka.messaging

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.routing.RoundRobinRouter
import akka.routing.DefaultResizer
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration._
import akka.actor.Props
import java.io.File
import scala.xml.XML

class masterMessagingActor extends Actor with ActorLogging {
  
   def mapForActorRef = messagingActorMap.messagingActorRefMap;
   
   var actorLower : Int = 1;
   var actorUpper : Int = 10;
   
  
  override def preStart() {
    //val resizer = new DefaultResizer(lowerBound = 1,upperBound = 10);
    // Initialize children actors here
    //put the actors in a map
    
    log.info("Starting masterMessagingActor (Message Master) instance hashcode # {}", this.hashCode());
    log.info("Configuring masterMessagingActor (Message Master) child actors hashcode # {}", this.hashCode());
    
    actorConfig();
    val resizer = new DefaultResizer(lowerBound = actorLower, upperBound = actorUpper);
    
    //multiple instances
    val subsciptManager = context.actorOf(Props[subscriptionManagerActor].withRouter(RoundRobinRouter(resizer = Some(resizer), supervisorStrategy = supervisorEscalator)), name = "subscriptionActor");
    mapForActorRef.putActor("subscriptionActor", subsciptManager);
    //multiple instances
    val broadcastActor = context.actorOf(Props[broadcasterManagerActor].withRouter(RoundRobinRouter(resizer = Some(resizer), supervisorStrategy = supervisorEscalator)), name = "broadcasterManagerActor");
    mapForActorRef.putActor("broadcasterManagerActor", broadcastActor);
    //single instance
    val topicAdminstatorActor = context.actorOf(Props[topicManagementWorkActor], name = "topicAdminActor");
    mapForActorRef.putActor("topicAdminActor", topicAdminstatorActor);
    //multiple instances
    val topicManagementActor = context.actorOf(Props(new topicManagerActor(topicAdminstatorActor)).withRouter(RoundRobinRouter(resizer = Some(resizer), supervisorStrategy = supervisorEscalator)), name = "topicManagerActor");
    mapForActorRef.putActor("topicManagerActor", topicManagementActor);
    
  }
  
   //Overriding postRestart to disable the call to preStart() after restarts
  override def postRestart(reason: Throwable): Unit = ()
  
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
  
  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 100, withinTimeRange = 10 seconds) {
    //For actors with a single instance
    case _: Exception => Restart
  }
  
  
  def receive ={
    case _=> log.info("unknown message");
  }
  
  
  def actorConfig() : Unit = {
    var workingDirectory = new java.io.File(".").getCanonicalPath();
    var folderPath: String = workingDirectory + "/config/Actors/";
    var configFolder = new File(folderPath);
    
    if (configFolder.exists()){
      //store actor information
      var storageXML : scala.xml.Elem = readXMLFile(folderPath, "messaging.xml");
      //readStoreContent(storageXML);
      
      if (storageXML != null){
              
        val actorLowerBound = (storageXML \"messagingActor"\ "lowerBound").text;
        val actorUpperBound = (storageXML \"messagingActor"\ "upperBound").text;
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