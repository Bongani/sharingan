package akka.router.routingActor

import akka.actor.ActorLogging
import akka.actor.Actor
import messages.workerMessage
//import akka.router.channelMap
import akka.actor.ActorRef
import org.eligosource.eventsourced.core._
import org.mashupbots.socko.events.WebSocketFrameEvent
import java.util.HashMap
import java.util.Map
/*maps the websocket event in case a response is expected
 * The frame request should come with an identifer if the request expects a response
 */
class clientLogWebSocketEventFrame(messageRoutingActor : ActorRef, extension : EventsourcingExtension, processorID : Int) extends Actor with ActorLogging{
  
  //def clientChannelSocketMap = channelMap.webSocketChannelSocketMap; 
  var clientChannelSocketMap : Map[Int, WebSocketFrameEvent] = new HashMap[Int, WebSocketFrameEvent];
  def receive = {
    case evtSourcedMessage: Message =>{
      val workMessage: workerMessage = evtSourcedMessage.event.asInstanceOf[workerMessage];
      //add client
      //log client information
     //channel will be used as the key
      val clientChannel: Int = workMessage.websocketEvent.channel.getId();
     
      val clientWebSocketEvent = workMessage.websocketEvent;
      clientChannelSocketMap.put(clientChannel, clientWebSocketEvent);    
      
      messageRoutingActor ! workMessage;
    }
    
    case clientChannelRequest: requestChannel => {
      val clientWebSocket : WebSocketFrameEvent = clientChannelSocketMap.get(clientChannelRequest.clientChannel);
      sender ! clientWebSocket;
    }
    
    case clientChannel : Int => {
      //remove client
      clientChannelSocketMap.remove(clientChannel);
    }
    
    case sr @ SnapshotRequest(pid, snr, _) => {
        sr.process(clientChannelSocketMap)
        println(s"processed snapshot request for (snr = ${snr})")

      }
    case so @ SnapshotOffer(Snapshot(_, snr, time, cMap:  Map[Int, WebSocketFrameEvent])) => {
        clientChannelSocketMap = cMap
        println(s"accepted snapshot offer for (snr = ${snr} time = ${time}})")
      }
        
    case _=> log.info("unknown message")
  }
  
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
		  //need to overide with empty method so eventsourced recovery works
    }
  
  override def postRestart(reason: Throwable): Unit = {
    println("\n recovering messages for actor crashing\n")
    extension.recover(Seq(ReplayParams(processorID, snapshot = true)))
  }
  

}