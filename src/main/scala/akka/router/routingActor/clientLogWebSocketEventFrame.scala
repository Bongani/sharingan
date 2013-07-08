package akka.router.routingActor

import akka.actor.ActorLogging
import akka.actor.Actor
import messages.workerMessage
import akka.router.channelMap
import akka.actor.ActorRef
/*maps the websocket event in case a response is expected
 * The frame request should come with an identifer if the request expects a response
 */
class clientLogWebSocketEventFrame(messageRoutingActor : ActorRef) extends Actor with ActorLogging{
  
  def clientChannelSocketMap = channelMap.webSocketChannelSocketMap; 
  
  def receive = {
    case workMessage: workerMessage =>{
      //add client
      //log client information
     //channel will be used as the key
      val clientChannel: Int = workMessage.websocketEvent.channel.getId();
     
      val clientWebSocketEvent = workMessage.websocketEvent;
      clientChannelSocketMap.putClientWebSocket(clientChannel, clientWebSocketEvent);    
      
      messageRoutingActor ! workMessage;
    }
    
    case clientChannel : Int => {
      //remove client
      clientChannelSocketMap.removeClientWebSocket(clientChannel);
    }
        
    case _=> log.info("unknown message")
  }
  

}