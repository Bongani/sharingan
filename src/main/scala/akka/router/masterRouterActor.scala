package akka.router

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.routing.SmallestMailboxRouter
import akka.routing.DefaultResizer
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration._
import akka.actor.Props
import akka.router.routingActor.routeMessageActor
import akka.router.adminActors.adminWorkerActor
import akka.router.routingActor.clientLogWebSocketEventFrame
import akka.router.routingActor.routerActor
import java.io.File
import scala.xml.XML
import org.eligosource.eventsourced.core._
import akka.actor.ActorRef
import java.util.HashMap
import java.util.Map
import akka.webserver.actorRequest


class masterRouterActor(extension : EventsourcingExtension, subSystemID: Int) extends Actor with ActorLogging{
  
  //def mapForActorRefRouter = routerActorMap.routerActorRefMap;
  var mapForActorRefRouter: Map[String, ActorRef] = new HashMap[String, ActorRef];
  
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
    
    
    
    //Worker actors for dispatcher Actor
    //single instance
    val routingAdmin: Int =subSystemID + 3;
    val routingAdminActor = extension.processorOf(Props(new adminWorkerActor(extension, routingAdmin) with Eventsourced { val id = routingAdmin} ))
    //recover actor process
    extension.recover(Seq(ReplayParams(routingAdmin, snapshot = true)));
    //val routingAdminActor = context.actorOf(Props[adminWorkerActor], name = "adminWorkerActor");
    
    
    //Worker actors for router Actor
    //multiple instances
    val messageRoutingActor = context.actorOf(Props(new routeMessageActor(routingAdminActor)).withRouter(SmallestMailboxRouter(resizer = Some(resizer), supervisorStrategy = supervisorEscalator)), name = "routeMessageActor");
    mapForActorRefRouter.put("routeMessageActor", messageRoutingActor);
    //single instance
    val clientLogID: Int =subSystemID + 2;
    val clientLogActor = extension.processorOf(Props(new clientLogWebSocketEventFrame(messageRoutingActor, extension, clientLogID) with Eventsourced { val id = clientLogID} ));
      //context.actorOf(Props(new clientLogWebSocketEventFrame(messageRoutingActor)),"clientLogWebSocketActor");
    //recover actor process
    extension.recover(Seq(ReplayParams(clientLogID, snapshot = true)));
    
    mapForActorRefRouter.put("clientLogWebSocketActor", clientLogActor);
    //multiple instances
    val routingActor = context.actorOf(Props(new routerActor(clientLogActor, messageRoutingActor)).withRouter(SmallestMailboxRouter(resizer = Some(resizer), supervisorStrategy = supervisorEscalator)), name = "routerActor");
    mapForActorRefRouter.put("routerActor", routingActor);
    
    
    mapForActorRefRouter.put("adminWorkerActor", routingAdminActor);
    //multiple instances
    val routerDispatchActor = context.actorOf(Props(new routerDispatcherActor(clientLogActor, routingAdminActor)).withRouter(SmallestMailboxRouter(resizer = Some(resizer), supervisorStrategy = supervisorEscalator)),"routerDispatcherActor");
    mapForActorRefRouter.put("routerDispatcherActor", routerDispatchActor);
    
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
    case request: actorRequest =>{
      val requestedActor: ActorRef = mapForActorRefRouter.get(request.actorName);
      sender ! requestedActor
    } 
    case _=> log.info("unknown message");
  }
  
  def actorConfig() : Unit = {
    var workingDirectory = new java.io.File(".").getCanonicalPath();
    var folderPath: String = workingDirectory + "/config/Actors/";
    var configFolder = new File(folderPath);
    
    if (configFolder.exists()){
      //store actor information
      var routingXML : scala.xml.Elem = readXMLFile(folderPath, "routing.xml");
      //readStoreContent(storageXML);
      
      if (routingXML != null){
              
        val actorLowerBound = (routingXML \"routerActor"\ "lowerBound").text;
        val actorUpperBound = (routingXML \"routerActor"\ "upperBound").text;
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