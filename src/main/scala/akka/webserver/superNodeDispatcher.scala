package akka.webserver

import akka.actor.ActorLogging
import akka.actor.Actor
import org.mashupbots.socko.routes._
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig
import java.util.Date
import org.mashupbots.socko.events.HttpResponseStatus
import java.io.File
import org.mashupbots.socko.webserver.SslConfig
import org.mashupbots.socko.webserver.HttpConfig
import org.mashupbots.socko.webserver.WebLogConfig
import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.Await
import scala.util.{Success, Failure}
import scala.concurrent.TimeoutException
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.actorRef2Scala
import akka.actor.ActorRef
import akka.node.systemNode
import org.mashupbots.socko.events.WebSocketFrameEvent
import org.mashupbots.socko.events.WebSocketHandshakeEvent

sealed trait superNodeDispatcherEvents
case class nodeRequest(nodeName: String) extends superNodeDispatcherEvents;
case class superNodeWebSocketFrame(node : systemNode, subSystemName : String, actorName : String, wsFRame : WebSocketFrameEvent) extends superNodeDispatcherEvents;
case class superNodeWebRequestWithPath(node : systemNode, subSystemName : String, actorName : String, wsHandshake : WebSocketHandshakeEvent, path : String) extends superNodeDispatcherEvents;
case class superNodeWebBroadcastWithPath(node : systemNode, subSystemName : String, actorName : String, wsFRame : WebSocketFrameEvent, path : String) extends superNodeDispatcherEvents;
case class superNodeWebSocketFrameStorage(node : systemNode, wsFRame : WebSocketFrameEvent) extends superNodeDispatcherEvents;

class superNodeDispatcher(actorSystem: ActorSystem, nodeManagerActor: ActorRef, dispatherForNodeActor : ActorRef) extends Actor with ActorLogging {
  
  implicit var timeout = Timeout(5 seconds);
  
  //var operationList : List[String] = new ArrayList[String];
  //operationList.add(0, "storage");
  val operationList = List("storage")
  
  val routes = Routes({
   
      case HttpRequest(httpRequest) => httpRequest match {
      case GET(Path("/html")) => {
        // Return HTML page to establish web socket
        httpRequest.response.write("Hello from Socko (" + new Date().toString + ")")
      }
      case Path("/favicon.ico") => {
        // If favicon.ico, just return a 404 because we don't have that file
        httpRequest.response.write(HttpResponseStatus.NOT_FOUND)
        //println(operationList(0))
      }
    }
    
   case WebSocketHandshake(wsHandshake) => wsHandshake match{
      //To start Web Socket processing we first have to authorize the handshake
      //This is a security measure to make sure that web sockets can only be established at your specified end points
     
     case PathSegments(nodeName :: "storage" :: Nil) => {
       //retrieve relative node
       val node: systemNode = retrieveNode(nodeName);
       if (node != null){
          wsHandshake.authorize();
       }       
        
      }
     
     //for routerDispatcherActor
     case PathSegments(nodeName :: "worker" :: Nil) => {
       val node: systemNode = retrieveNode(nodeName);
       if (node != null){
          wsHandshake.authorize();
       }    
      }
     
     //for routerActor
     case PathSegments(nodeName :: "computation" :: Nil) => {
       val node: systemNode = retrieveNode(nodeName);
       if (node != null){
          wsHandshake.authorize();
       }    
      }
     
     
     //for topicManagerActor
     case PathSegments(nodeName :: "topicmanagement" :: Nil) => {
       val node: systemNode = retrieveNode(nodeName);
       if (node != null){
          wsHandshake.authorize();
       }    
      }

     //What?
      //case Path("/messagingtopic") => {
      //  wsHandshake.authorize();
      //}
      
      //subscription handshake
      case PathSegments(nodeName :: "messaging" :: relativePath) => {
        //System.out.println("\n \n Hello \n \n");
       val node: systemNode = retrieveNode(nodeName);
       if (node != null){
         val request = new superNodeWebRequestWithPath(node, "messaging", "subscriptionActor", wsHandshake, relativePath(0));
         //subscriptionRequest(wsHandshake, relativePath(0));
        
        //superNodeWebRequestWithPath
         dispatherForNodeActor ! request;
                  
       }
        
      }
      
    }
    
    case WebSocketFrame(wsFrame) => wsFrame match {
      
      case PathSegments(nodeName :: "storage" :: Nil) => {
        val node: systemNode = retrieveNode(nodeName);
        if (node != null){
          val message = new superNodeWebSocketFrameStorage(node, wsFrame);
          dispatherForNodeActor ! message   
       }        
      }
      
      //for workerManagerActor
     case PathSegments(nodeName :: "worker" :: Nil) => {
        //routerDispatchActor ! wsFrame;
        val node: systemNode = retrieveNode(nodeName);
        if (node != null){
          val message = new superNodeWebSocketFrame(node,"router", "routerDispatcherActor", wsFrame);
          dispatherForNodeActor ! message   
       } 
      }
      
       //for routerActor
      case PathSegments(nodeName :: "computation" :: Nil ) => {
        //routingActor ! wsFrame;
        //println("\n Sending to router to find worker \n");
        val node: systemNode = retrieveNode(nodeName);
        if (node != null){
          val message = new superNodeWebSocketFrame(node,"router", "routerActor", wsFrame);
          dispatherForNodeActor ! message   
       } 
      }
      
      //for topicManagerActor
      case PathSegments( nodeName :: "topicmanagement" ::  Nil) => {
       //topicManagementActor ! wsFrame;
        val node: systemNode = retrieveNode(nodeName);
        if (node != null){
          val message = new superNodeWebSocketFrame(node,"messaging", "topicManagerActor", wsFrame);
          dispatherForNodeActor ! message   
       } 
      }
      
      //broadcast message
      case PathSegments(nodeName :: "messaging" :: relativePath) => {
        //superNodeWebBroadcastWithPath(node : systemNode, subSystemName : String, actorName : String, wsFRame : WebSocketFrameEvent, path : String)
        //val broadcastRequest = new broadcastMessage(wsFrame, relativePath(0));        
        //broadcastActor ! broadcastRequest;    
        
        val node: systemNode = retrieveNode(nodeName);
        if (node != null){
          val broadcastRequest = new superNodeWebBroadcastWithPath(node, "messaging", "broadcasterManagerActor", wsFrame, relativePath(0));
         
          dispatherForNodeActor ! broadcastRequest                  
       }
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
  
 
  def retrieveNode(nodeName: String): systemNode ={
    val msg = new nodeRequest(nodeName);
    var nodeRetrieved: systemNode = null; 
    nodeManagerActor ? msg onComplete {
      case Success(retrievedNode : systemNode) => {
              nodeRetrieved = retrievedNode;         
            }
            case Failure(e: TimeoutException) => {
              log.info("failed to retrieve client channel");
              //return null;

            }
          }
    return nodeRetrieved;

  }
  
  
  /*def registerActors(): Unit = {
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
    
        
  }*/

}