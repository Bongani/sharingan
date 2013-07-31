package akka.router.routingActor

import akka.actor.ActorLogging
import akka.actor.Actor
import org.mashupbots.socko.events.WebSocketFrameEvent
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import messages.workerMessage
import akka.router.routingActor.routeMessageActor
import akka.actor.Props
import akka.actor.ActorRef
import org.eligosource.eventsourced.core._


case class jsonRouteMessage(worker: String, operationData: String, response: Boolean);

//can have multiple instances of router
class routerActor(clientLogActor : ActorRef, messageRoutingActor : ActorRef) extends Actor with ActorLogging {
  
  implicit val formats = DefaultFormats; // Brings in default date formats etc for JSON Lift
  
  
   //creating actors
  //val resizer = new DefaultResizer(lowerBound = 2,upperBound = 10);
  //val messageRoutingActor = context.actorOf(Props[routeMessageActor].withRouter(RoundRobinRouter(resizer = Some(resizer))), name = "routeMessageActor");
  //val clientLogActor = context.actorOf(Props(new clientLogWebSocketEventFrame(messageRoutingActor)),"clientLogWebSocketActor")
  //Props(new topicManagerActor(topicAdminstatorActor))
  
  def receive = {
    case websocketEvent: WebSocketFrameEvent => {
      
      val messageStringfied = websocketEvent.readText;
      val workMessage: workerMessage = decodeRouteMessage(messageStringfied, websocketEvent);
      
      //if client is expecting a response
      println("work message: " + workMessage);
      if (workMessage.expectingResponse){
        clientLogActor ! Message(workMessage);
      } else {
        messageRoutingActor ! workMessage;
      }
      
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
  
  override def preStart() {
    log.info("Starting routerActor (routerActor under masterRouterActor) instance hashcode # {}", this.hashCode());  
  }
  
  override def postStop() {
    log.info("Stopping routerActor (routerActor under masterRouterActor) instance hashcode # {}",this.hashCode());
  }

}