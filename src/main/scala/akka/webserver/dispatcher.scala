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
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.Await
import scala.util.{Success, Failure}
import scala.concurrent.TimeoutException
import akka.dispatch.Futures
import akka.dispatch.OnComplete
import scala.concurrent.ExecutionContext.Implicits.global
import test.actorTest

//dispatcher actors internal messages
sealed trait dispatcherEvents
//message to send to subscription manager when a node wishes to subscribe to a topic val webSocketBroadcaste
case class subscriptionRequest(websockHandshake: WebSocketHandshakeEvent, subscriptionID: String) extends dispatcherEvents;
case class broadcastMessage(websockEvent: WebSocketFrameEvent, topicID: String) extends dispatcherEvents;
//messages to request actors from router master & message master
case class actorRequest(actorName : String) extends dispatcherEvents;

class dispatcher(dipatcherChoice : Int, systemPort: Int, actorSystem: ActorSystem, storageActor: ActorRef, messagingMaster: ActorRef, routingMaster: ActorRef) extends Actor with ActorLogging {
  
  implicit var timeout = Timeout(5 seconds);
  
//  def mapForMessagingActorRef = messagingActorMap.messagingActorRefMap;
// def mapForRoutingActorRef = routerActorMap.routerActorRefMap;
  
  var subsrciptManager: ActorRef = null;
  var topicManagementActor: ActorRef = null;
  var broadcastActor: ActorRef = null;
  
  //Worker actors for router Actor
  var routingActor: ActorRef = null;//actorSystem.actorOf(Props(new routerActor(clientLogActor, messageRoutingActor)), name = "routerActor");
  //Worker actors for dispatcher Actor  
  var routerDispatchActor: ActorRef = null;//actorSystem.actorOf(Props(new routerDispatcherActor(clientLogActor, routingAdminActor)),"routerDispatcherActor");
  
  var helloActor: ActorRef = actorSystem.actorOf(Props[actorTest]);
  
  
  val routes = Routes({
   
      case HttpRequest(httpRequest) => httpRequest match {
      case GET(Path("/html")) => {
        // Return HTML page to establish web socket
        //httpRequest.response.write("Hello from Socko (" + new Date().toString + ")")
        helloActor ! httpRequest
      }
      case GET(Path("/htmltest2")) => {
        // Return HTML page to establish web socket
        //httpRequest.response.write("Hello from Socko (" + new Date().toString + ")")
        helloActor ! httpRequest
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
       //println("Testing if this works")
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
      case Path("/service") => {
        routerDispatchActor ! wsFrame;
      }
      
       //for routerActor
      case Path("/computation") => {
        routingActor ! wsFrame;
        println("\n Sending to router to find worker \n");
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
    
    if (dipatcherChoice == 1){
      //SPDY setup
      val workingDirectory = new java.io.File(".").getCanonicalPath();
      var folderPath: String = workingDirectory + "/keystore/server.jks";
  
     val keyStoreFile = new File(folderPath);  
     val keyStoreFilePassword = "password";
     val sslConfig = SslConfig(keyStoreFile, keyStoreFilePassword, None, None);
     val httpConfig = HttpConfig(spdyEnabled = true);
     val webServerConfig = WebServerConfig(hostname="0.0.0.0",port= systemPort,webLog = Some(WebLogConfig()), ssl = Some(sslConfig), http = httpConfig)
  
  
    		 val webServer = new WebServer(webServerConfig, routes, actorSystem);
  
  //val webServer = new WebServer(WebServerConfig(), routes, actorSystem);
    
     
     webServer.start();
     println("Server started");    
     registerActors;
     println("Connect to https://hostname:+"+ systemPort + "/html to establish connection")
    }
    
    if (dipatcherChoice == 2){
      //secure
      val workingDirectory = new java.io.File(".").getCanonicalPath();
      var folderPath: String = workingDirectory + "/keystore/server.jks";  
     val keyStoreFile = new File(folderPath);  
     val keyStoreFilePassword = "password";
      
     val sslConfig = SslConfig(keyStoreFile, keyStoreFilePassword, None, None)
      val webServer = new WebServer(WebServerConfig(hostname="0.0.0.0",port=systemPort,ssl = Some(sslConfig)), routes, actorSystem)
     
      //val webServer = new WebServer(WebServerConfig(hostname="0.0.0.0",port=7777), routes, actorSystem);
      webServer.start();
      println("Server started");
      registerActors;
      println("Connect to https://hostname:" + systemPort +"/html to establish connection")
    }   
    
     if (dipatcherChoice == 3){
      //secure

      val webServer = new WebServer(WebServerConfig(hostname="0.0.0.0",port=systemPort), routes, actorSystem);
      webServer.start();
      println("Server started");
      registerActors;
      println("Connect to http://hostname:" + systemPort + "/html to establish connection")
    } 
  
  }
  
  override def postStop(){
    //webServer.stop();
    //contentDir.delete();    
  }
  
  def receive ={
    case _=> log.info("unknown message");
  }
  
  def registerActors(): Unit = {
    //get ActorRef Actors
    
    //message Actors
    
  //subsrciptManager = mapForMessagingActorRef.getActor("subscriptionActor");
    val subsMsg = new actorRequest("subscriptionActor");
    messagingMaster ? subsMsg onComplete {
      case Success(smActor : ActorRef) => {
        if (smActor != null){
          subsrciptManager = smActor
                
              } else {
                log.info("Actor does not exist");
              }           
            }
            case Failure(e: TimeoutException) => {
              log.info("failed to retrieve actor");

            }
          }
    
  //topicManagementActor = mapForMessagingActorRef.getActor("topicManagerActor");
    val topMsg = new actorRequest("topicManagerActor");
    messagingMaster ? topMsg onComplete {
      case Success(topManActor : ActorRef) => {
        if (topManActor != null){
          topicManagementActor = topManActor;
                
              } else {
                log.info("Actor does not exist");
              }           
            }
            case Failure(e: TimeoutException) => {
              log.info("failed to retrieve actor");

            }
          }
    
  //broadcastActor = mapForMessagingActorRef.getActor("broadcasterManagerActor");
    val broadcastMsg = new actorRequest("broadcasterManagerActor");
    messagingMaster ? broadcastMsg onComplete {
      case Success(broadActor : ActorRef) => {
        if (broadActor != null){
          broadcastActor = broadActor;
                
              } else {
                log.info("Actor does not exist");
              }           
            }
            case Failure(e: TimeoutException) => {
              log.info("failed to retrieve actor");

            }
          }
    
    
    
  //routing Actors
    
  //routingActor = mapForRoutingActorRef.getActor("routerActor");
    val routingMsg = new actorRequest("routerActor");
    routingMaster ? routingMsg onComplete {
      case Success(routeActor : ActorRef) => {
        if (routeActor != null){
          routingActor = routeActor;
                
              } else {
                log.info("Actor does not exist");
              }           
            }
            case Failure(e: TimeoutException) => {
              log.info("failed to retrieve actor");

            }
          }
    
  //routerDispatchActor = mapForRoutingActorRef.getActor("routerDispatcherActor");
    val routeDispatchMsg = new actorRequest("routerDispatcherActor");
    routingMaster ? routeDispatchMsg onComplete {
      case Success(rdActor : ActorRef) => {
        if (rdActor != null){
          routerDispatchActor = rdActor;
                
              } else {
                log.info("Actor does not exist");
              }           
            }
            case Failure(e: TimeoutException) => {
              log.info("failed to retrieve actor");

            }
          }
    
        
  }
  

}