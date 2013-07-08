package akka.webserver

import akka.actor.ActorLogging
import akka.actor.Actor
import org.mashupbots.socko.routes._
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig
import java.util.Date
import org.mashupbots.socko.events.HttpResponseStatus

class superNodeDispatcher extends Actor with ActorLogging {
  
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
     case PathSegments( nodeID :: "storage") => {
        wsHandshake.authorize();
        
      }
     
     //for routerDispatcherActor
     case PathSegments( nodeID :: "worker") => {
        wsHandshake.authorize();
      }
     
     //for routerActor
     case PathSegments( nodeID :: "computation") => {
        wsHandshake.authorize();
      }
     
     
     //for topicManagerActor
     case PathSegments( nodeID :: "topicmanagement") => {
        wsHandshake.authorize();
      }

     //What?
      //case Path("/messagingtopic") => {
      //  wsHandshake.authorize();
      //}
      
      //subscription handshake
      case PathSegments(nodeID :: "messaging" :: relativePath) => {
        //System.out.println("\n \n Hello \n \n");
        val request = new subscriptionRequest(wsHandshake, relativePath(0));
        subsrciptManager ! request;
        
      }
      
    }
    
    case WebSocketFrame(wsFrame) => wsFrame match {
      
      case PathSegments(nodeID :: "storage") => {
        storageActor ! wsFrame;       
        
      }
      
      //for workerManagerActor
      case Path("/worker") => {
        routerDispatchActor ! wsFrame;
      }
      
       //for routerActor
      case PathSegments(nodeID :: "computation") => {
        routingActor ! wsFrame;
        println("\n Sending to router to find worker \n");
      }
      
      //for topicManagerActor
      case PathSegments( nodeID :: "topicmanagement") => {
       topicManagementActor ! wsFrame;
      }
      
      //broadcast message
      case PathSegments(nodeID :: "messaging" :: relativePath) => {
        val broadcastRequest = new broadcastMessage(wsFrame, relativePath(0));        
        broadcastActor ! broadcastRequest;        
      }
      
    }
    
  })

}