package akka.router.routingActor

import akka.actor.Actor
import akka.actor.ActorLogging
import org.mashupbots.socko.events.WebSocketFrameEvent
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import akka.actor.ActorRef
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.Await
import scala.util.{Success, Failure}
import scala.concurrent.TimeoutException
import akka.dispatch.Futures
import akka.dispatch.OnComplete
import scala.concurrent.ExecutionContext.Implicits.global

sealed trait forwardResultEvents
case class requestChannel(clientChannel: Int) extends forwardResultEvents;

class forwardResultActor(clientLogActor : ActorRef) extends Actor with ActorLogging {
  
  
  implicit var timeout = Timeout(5 seconds);
  //def clientChannelSocketMap = channelMap.webSocketChannelSocketMap; 
  
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
    
    val msg = new requestChannel(clientChannel);
    clientLogActor ? msg onComplete {
            case Success(clientWebSocket : WebSocketFrameEvent) => {
              if (clientWebSocket != null){
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
            case Failure(e: TimeoutException) => {
              log.info("failed to retrieve client channel");

            }
          }
    
    //get client channel websocket
   /* if (clientChannelSocketMap.channelClientMap.containsKey(clientChannel)){
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
      
    }*/
    
  }
  
  override def preStart() {
    log.info("Starting forwardResultActor (forwardResultActor under forwardDecoderActor) instance hashcode # {}", this.hashCode());  
  }
  
  override def postStop() {
    log.info("Stopping forwardResultActor (forwardResultActor under forwardDecoderActor) instance hashcode # {}",this.hashCode());
  }

}