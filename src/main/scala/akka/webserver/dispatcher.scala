package akka.webserver

import akka.actor.Actor
import akka.actor.ActorLogging
import org.mashupbots.socko.routes._
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig
import java.io.File
import org.mashupbots.socko.webserver.SslConfig
import org.mashupbots.socko.webserver.HttpConfig
import org.mashupbots.socko.webserver.WebLogConfig
import akka.actor.ActorRef
import akka.actor.ActorSystem
import org.mashupbots.socko.webserver.SslConfig
import org.mashupbots.socko.events.HttpResponseStatus
import java.util.Date
import org.mashupbots.socko.routes._
import java.io.File
import akka.actor.actorRef2Scala
import org.mashupbots.socko.events.WebSocketHandshakeEvent
import org.mashupbots.socko.events.WebSocketFrameEvent
import akka.actor.Props
import akka.router.adminActors.workerManagerActor
import akka.messaging.subscriptionManagerActor
import akka.messaging.broadcasterManagerActor
import akka.messaging.topicManagerActor
import akka.router.adminActors.adminWorkerActor
import akka.router.routerDispatcherActor
import akka.router.routingActor.routerActor
import akka.router.routingActor.clientLogWebSocketEventFrame
import akka.router.routingActor.routeMessageActor
import akka.messaging.topicManagementWorkActor
import akka.messaging.messagingActorMap
import akka.router.routerActorMap

//dispatcher actors internal messages
sealed trait dispatcherEvents
//message to send to subscription manager when a node wishes to subscribe to a topic val webSocketBroadcaste
case class subscriptionRequest(websockHandshake: WebSocketHandshakeEvent, subscriptionID: String) extends dispatcherEvents;
case class broadcastMessage(websockEvent: WebSocketFrameEvent, topicID: String) extends dispatcherEvents;

class dispatcher(actorSystem: ActorSystem, storageActor: ActorRef) extends Actor with ActorLogging {
  
  def mapForMessagingActorRef = messagingActorMap.messagingActorRefMap;
  def mapForRoutingActorRef = routerActorMap.routerActorRefMap;
  
  var subsrciptManager: ActorRef = null;
  var topicManagementActor: ActorRef = null;
  var broadcastActor: ActorRef = null;
  
  //Worker actors for router Actor
  var routingActor: ActorRef = null;//actorSystem.actorOf(Props(new routerActor(clientLogActor, messageRoutingActor)), name = "routerActor");
  //Worker actors for dispatcher Actor  
  var routerDispatchActor: ActorRef = null;//actorSystem.actorOf(Props(new routerDispatcherActor(clientLogActor, routingAdminActor)),"routerDispatcherActor");
  
  
  
  val routes = Routes({
   
      case HttpRequest(httpRequest) => httpRequest match {
      case GET(Path("/html")) => {
        // Return HTML page to establish web socket
        httpRequest.response.write("Hello from Socko (" + new Date().toString + ")")
      }
      case Path("/favicon.ico") => {
        // If favicon.ico, just return a 404 because we don't have that file
        httpRequest.response.write(HttpResponseStatus.NOT_FOUND)
      }
    }
    
   case WebSocketHandshake(wsHandshake) => wsHandshake match{
      //To start Web Socket processing we first have to authorize the handshake
      //This is a security measure to make sure that web sockets can only be established at your specified end points
     case Path("/storage") => {
        wsHandshake.authorize();
        
      }
     
     //for routerDispatcherActor
     case Path("/worker") => {
        wsHandshake.authorize();
      }
     
     //for routerActor
     case Path("/computation") => {
        wsHandshake.authorize();
      }
     
     
     //for topicManagerActor
     case Path("/topicmanagement") => {
        wsHandshake.authorize();
      }

     //What?
      //case Path("/messagingtopic") => {
      //  wsHandshake.authorize();
      //}
      
      //subscription handshake
      case PathSegments("messaging" :: relativePath) => {
        //System.out.println("\n \n Hello \n \n");
        val request = new subscriptionRequest(wsHandshake, relativePath(0));
        subsrciptManager ! request;
        
      }
      
    }
    
    case WebSocketFrame(wsFrame) => wsFrame match {
      
      case Path("/storage") => {
        storageActor ! wsFrame;       
        
      }
      
      //for workerManagerActor
      case Path("/worker") => {
        routerDispatchActor ! wsFrame;
      }
      
       //for routerActor
      case Path("/computation") => {
        routingActor ! wsFrame;
      }
      
      //for topicManagerActor
      case Path("/topicmanagement") => {
       topicManagementActor ! wsFrame;
      }
      
      //broadcast message
      case PathSegments("messaging" :: relativePath) => {
        val broadcastRequest = new broadcastMessage(wsFrame, relativePath(0));        
        broadcastActor ! broadcastRequest;        
      }
      
    }
    
  })
  
  
  
  
  
  override def preStart(){
    //SPDY setup
  
  val keyStoreFile = new File("/home/bongani/Documents/server.jks");  
  val keyStoreFilePassword = "password";
  val sslConfig = SslConfig(keyStoreFile, keyStoreFilePassword, None, None);
  val httpConfig = HttpConfig(spdyEnabled = true);
  val webServerConfig = WebServerConfig(hostname="0.0.0.0", webLog = Some(WebLogConfig()), ssl = Some(sslConfig), http = httpConfig)
  
  
  val webServer = new WebServer(webServerConfig, routes, actorSystem);
  
  //val webServer = new WebServer(WebServerConfig(), routes, actorSystem);
    
     
  webServer.start();
  println("Server started");
    
  
  //get ActorRef Actors
  //message Actors
  subsrciptManager = mapForMessagingActorRef.getActor("subscriptionActor");
  topicManagementActor = mapForMessagingActorRef.getActor("topicManagerActor");
  broadcastActor = mapForMessagingActorRef.getActor("broadcasterManagerActor");
  //routing Actors
  routingActor = mapForRoutingActorRef.getActor("routerActor");
  routerDispatchActor = mapForRoutingActorRef.getActor("routerDispatcherActor");
    
  }
  
  override def postStop(){
    //webServer.stop();
    //contentDir.delete();    
  }
  
  def receive ={
    case _=> log.info("unknown message");
  }
  

}