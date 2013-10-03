package latency.transport


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
//import akka.messaging.messagingActorMap
//import akka.router.routerActorMap
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.Await
import scala.util.{Success, Failure}
import scala.concurrent.TimeoutException
import akka.dispatch.Futures
import akka.dispatch.OnComplete
import scala.concurrent.ExecutionContext.Implicits.global

//dispatcher actors internal messages
sealed trait dispatcherEvents
//message to send to subscription manager when a node wishes to subscribe to a topic val webSocketBroadcaste
case class subscriptionRequest(websockHandshake: WebSocketHandshakeEvent, subscriptionID: String) extends dispatcherEvents;
case class broadcastMessage(websockEvent: WebSocketFrameEvent, topicID: String) extends dispatcherEvents;
//messages to request actors from router master & message master
case class actorRequest(actorName : String) extends dispatcherEvents;

class dispatcher(actorSystem: ActorSystem, HelloHandlerActor : ActorRef) extends Actor with ActorLogging {
  
 
  
  val routes = Routes({
   case GET(request) => {
      HelloHandlerActor ! request
    }    
  })
  
  
  
  
  
  override def preStart(){
    //SPDY setup
  
  /*val keyStoreFile = new File("/home/bongani/Documents/server.jks");  
  val keyStoreFilePassword = "password";
  val sslConfig = SslConfig(keyStoreFile, keyStoreFilePassword, None, None);
  val httpConfig = HttpConfig(spdyEnabled = true);
  val webServerConfig = WebServerConfig(hostname="0.0.0.0", webLog = Some(WebLogConfig()), ssl = Some(sslConfig), http = httpConfig)
  
  
  val webServer = new WebServer(webServerConfig, routes, actorSystem);*/
  
  val webServer = new WebServer(WebServerConfig(), routes, actorSystem);
    
     
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