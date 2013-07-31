package akka.messaging

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.webserver.broadcastMessage
import akka.actor.ActorRef
import org.mashupbots.socko.handlers.WebSocketBroadcastText
import akka.actor.actorRef2Scala
import akka.webserver.broadcastMessage
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
import messages.requestTopicBroadcastcActor


//sealed trait bmEvents
//case class retreiveTopicActor(broadcasterName: String) extends bmEvents

//can have multiple instances of this
class broadcasterManagerActor(topicAdminActor: ActorRef) extends Actor with ActorLogging{
  
//  def topicMapManager = topicMap.topicNameActorMap;
  
  implicit var timeout = Timeout(5 seconds);

  def receive = {
    case message : broadcastMessage =>{
      //broadcast message recieved
      messageBroadcaster(message);
    }    
    case _=> log.info("unknown message")
  }
  
  def messageBroadcaster(bMessage : broadcastMessage): Unit = {
    
    //get topic
    val msg = new requestTopicBroadcastcActor(bMessage.topicID);
    topicAdminActor ? msg onComplete {
      case Success(webSocketBroadcaster : ActorRef) => {
              if (webSocketBroadcaster != null){
                val message = bMessage.websockEvent.readText
                webSocketBroadcaster ! WebSocketBroadcastText(message);
                
              } else {
                log.info("Topic does not exist");
              }           
            }
            case Failure(e: TimeoutException) => {
              log.info("failed to retrieve client channel");

            }
          }
    
    
    
/*    if ((topicMapManager.tMap.containsKey(bMessage.topicID))){
      //topic exist
       val webSocketBroadcaster : ActorRef = topicMapManager.getActor(bMessage.topicID); 
       val message = bMessage.websockEvent.readText
       webSocketBroadcaster ! WebSocketBroadcastText(message);       
      
    } else {
      
     log.info("Topic does not exist")
    }*/  
       
  }
  
  override def preStart() {
    log.info("Starting broadcasterManagerActor (broadcasterManagerActor under masterMessagingActor) instance hashcode # {}", this.hashCode());  
  }
  
  override def postStop() {
    log.info("Stopping broadcasterManagerActor (broadcasterManagerActor under masterMessagingActor) instance hashcode # {}",this.hashCode());
  }
  
}