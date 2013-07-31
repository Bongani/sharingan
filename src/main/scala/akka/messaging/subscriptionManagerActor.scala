package akka.messaging

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.webserver.subscriptionRequest
import akka.actor.ActorRef
import org.mashupbots.socko.handlers.WebSocketBroadcasterRegistration
import akka.actor.actorRef2Scala
import messages.requestTopicBroadcastcActor
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.Await
import scala.util.{Success, Failure}
import scala.concurrent.TimeoutException
import akka.dispatch.Futures
import akka.dispatch.OnComplete
import scala.concurrent.ExecutionContext.Implicits.global



class subscriptionManagerActor (topicAdminActor: ActorRef) extends Actor with ActorLogging{
  
  
  //def topicMapManager = topicMap.topicNameActorMap;
  
  implicit var timeout = Timeout(5 seconds);
  
  def receive = {
    case message: subscriptionRequest => {
      subscribeToTopic(message);
    }
    case _=> log.info("unknown message")
  }
  
  def subscribeToTopic(subscriptionMessage: subscriptionRequest): Unit ={
    //check if topic exists
    
    val msg = new requestTopicBroadcastcActor(subscriptionMessage.subscriptionID);
    topicAdminActor ? msg onComplete {
      case Success(webSocketBroadcaster : ActorRef) => {
              if (webSocketBroadcaster != null){
                subscriptionMessage.websockHandshake.authorize();
                //subscribe handshake to topic
                webSocketBroadcaster ! new WebSocketBroadcasterRegistration(subscriptionMessage.websockHandshake);
                
              } else {
                log.info("Topic does not exist");
              }           
            }
            case Failure(e: TimeoutException) => {
              log.info("failed to retrieve client channel");

            }
          }
    
    
    /*if ((topicMapManager.tMap.containsKey(subscriptionMessage.subscriptionID))){
      //topic exist
      //createTopic(message.name, wsEvent);
      //authorize handshake
      subscriptionMessage.websockHandshake.authorize();
      
       val webSocketBroadcaster : ActorRef = topicMapManager.getActor(subscriptionMessage.subscriptionID); //topicMap.get(subscriptionMessage.subscriptionID);
       //subscribe handshake to topic
       webSocketBroadcaster ! new WebSocketBroadcasterRegistration(subscriptionMessage.websockHandshake);       
      
    } else {
      
     log.info("Topic does not exist")
    }*/    
    
  }
  
  override def preStart() {
    log.info("Starting subscriptionManagerActor (subscriptionManagerActor under masterMessagingActor) instance hashcode # {}", this.hashCode());  
  }
  
  override def postStop() {
    log.info("Stopping subscriptionManagerActor (subscriptionManagerActor under masterMessagingActor) instance hashcode # {}",this.hashCode());
  }

}