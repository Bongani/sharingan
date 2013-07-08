package akka.router.routingActor

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.router.channelMap
import org.mashupbots.socko.events.WebSocketFrameEvent
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import akka.actor.ActorRef

class forwardResultActor(clientLogActor : ActorRef) extends Actor with ActorLogging {
  
  def clientChannelSocketMap = channelMap.webSocketChannelSocketMap; 
  
  implicit val formats = DefaultFormats; // Brings in default date formats etc for JSON Lift
  
  def receive = {
    case message: forwardMessage =>{
      forwardMessage(message);      
    }
        
    case _=> log.info("unknown message")
  }
  
  def forwardMessage(fMessage : forwardMessage): Unit = {
    //get worker by channel
    val clientChannel : Int = fMessage.clientChannel;
    
    //get client channel websocket
    if (clientChannelSocketMap.channelClientMap.containsKey(clientChannel)){
      val clientWebSocket : WebSocketFrameEvent = clientChannelSocketMap.getClientWebSocket(clientChannel);
      
      //create json message worker, operationData
      val json = ("worker" -> fMessage.worker)~("operationData" -> fMessage.operationData);
      val returnMessage: String = compact(render(json));
      clientWebSocket.writeText(returnMessage);
      println("\n \n returning message \n" + returnMessage + "\n \n");
      //remove websocket
      clientLogActor ! clientChannel;
      
    } else {
      log.info("client to forward message from calculation does not exist");
      
    }
    
  }
  
  override def preStart() {
    log.info("Starting forwardResultActor (forwardResultActor under forwardDecoderActor) instance hashcode # {}", this.hashCode());  
  }
  
  override def postStop() {
    log.info("Stopping forwardResultActor (forwardResultActor under forwardDecoderActor) instance hashcode # {}",this.hashCode());
  }

}