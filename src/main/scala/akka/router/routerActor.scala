package akka.router

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.ActorRef
import org.mashupbots.socko.events.WebSocketFrameEvent
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import messages.workerMessage
import akka.routing.DefaultResizer
import akka.routing.RoundRobinRouter
import akka.actor.Props
import akka.router.routingActor.clientLogWebSocketEventFrame


case class jsonRouteMessage(worker: String, operationData: String, response: Boolean);

//can have multiple instances of router
class routerActor extends Actor with ActorLogging {
  
  implicit val formats = DefaultFormats; // Brings in default date formats etc for JSON Lift
  
  
   //creating actors
  val resizer = new DefaultResizer(lowerBound = 2,upperBound = 10);
  val messageRoutingActor = context.actorOf(Props[routeMessageActor].withRouter(RoundRobinRouter(resizer = Some(resizer))), name = "deleteActor");
  val clientLogActor = context.actorOf(Props[clientLogWebSocketEventFrame],"clientLogWebSocketActor")
  
  
  def receive = {
    case websocketEvent: WebSocketFrameEvent => {
      val messageStringfied = websocketEvent.readText;
      val workMessage: workerMessage = decodeRouteMessage(messageStringfied, websocketEvent);
      //if client is expecting a response
      if (workMessage.expectingResponse){
        clientLogActor ! workMessage;
      }
      messageRoutingActor ! workMessage;
    }
    
    case _=> log.info("unknown message")
  }
  
  def decodeRouteMessage(message : String, wsEvent: WebSocketFrameEvent): workerMessage = {
    val json = parse(message);      
    val jsonData = json.extract[jsonRouteMessage];
    
    val workerActor = jsonData.worker;
    val taskOperation = jsonData.operationData;
    val responseExpected = jsonData.response;
    
    val wMessage = new workerMessage(workerActor, taskOperation, wsEvent, responseExpected);
    return wMessage;
    
  }

}