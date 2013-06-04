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
import akka.router.workerManagerActor
import akka.messaging.subscriptionManagerActor
import akka.messaging.topicAdminActor
import akka.messaging.broadcasterManagerActor
import akka.messaging.topicManagerActor

//dispatcher actors internal messages
sealed trait dispatcherEvents
//message to send to subscription manager when a node wishes to subscribe to a topic val webSocketBroadcaste
case class subscriptionRequest(websockHandshake: WebSocketHandshakeEvent, subscriptionID: String) extends dispatcherEvents;
case class broadcastMessage(websockEvent: WebSocketFrameEvent, topicID: String) extends dispatcherEvents;

class dispatcher(actorSystem: ActorSystem, storageActor: ActorRef) extends Actor with ActorLogging {
  //extends Logger
  // workerAdminActor: ActorRef, subsciptManager: ActorRef, topicManagementActor : ActorRef, broadcastActor : ActorRef
  /*
   * val voldActor = system.actorOf(Props[voldCoordinator], name = "voldemorCordActor");
    val workActor = system.actorOf(Props[workerManagerActor], name = "workerManagerActor");
    val subscriptActor = system.actorOf(Props[subscriptionManagerActor], name = "subscriptionActor");
    val topicAdminstatorActor = system.actorOf(Props[topicAdminActor], name = "topicAdminActor");
    val topicManagementActor = system.actorOf(Props(new topicManagerActor(topicAdminstatorActor)), name = "topicAdminActor");
   */
  val workerAdminActor = actorSystem.actorOf(Props[workerManagerActor], name = "workerManagerActor");
  val subsciptManager = actorSystem.actorOf(Props[subscriptionManagerActor], name = "subscriptionActor");
  val topicAdminstatorActor = actorSystem.actorOf(Props[topicAdminActor], name = "topicAdminActor");
  val topicManagementActor = actorSystem.actorOf(Props(new topicManagerActor(topicAdminstatorActor)), name = "topicManagerActor");
  val broadcastActor = actorSystem.actorOf(Props[broadcasterManagerActor], name = "broadcasterManagerActor");
  
  
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
     
     //for workerManagerActor
     case Path("/workermanagement") => {
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
        subsciptManager ! request;
        
      }
      
    }
    
    case WebSocketFrame(wsFrame) => wsFrame match {
      
      case Path("/storage") => {
        storageActor ! wsFrame;       
        
      }
      
      //for workerManagerActor
      case Path("/workermanagement") => {
        workerAdminActor ! wsFrame;
      }
      
      //for topicManagerActor
      case Path("/topicmanagement") => {
        //println("\n \n \n");
        //println("got the message");
        //System.out.println("\n \n Hello \n \n");
       topicManagementActor ! wsFrame;
       //log.info("recieved topic management message");
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
    
    
    
  }
  
  override def postStop(){
    //webServer.stop();
    //contentDir.delete();    
  }
  
  def receive ={
    case _=> log.info("unknown message");
  }
  

}