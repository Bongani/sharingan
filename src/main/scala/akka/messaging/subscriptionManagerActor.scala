package akka.messaging

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.webserver.subscriptionRequest
import akka.actor.ActorRef
import org.mashupbots.socko.handlers.WebSocketBroadcasterRegistration
import akka.actor.actorRef2Scala



class subscriptionManagerActor extends Actor with ActorLogging{
  
  //var topicMap: Map[String, ActorRef] = new HashMap[String, ActorRef];
  
  def topicMapManager = topicMap.topicNameActorMap;
  
  def receive = {
    case message: subscriptionRequest => {
      subscribeToTopic(message);
    }
    case _=> log.info("unknown message")
  }
  
  def subscribeToTopic(subscriptionMessage: subscriptionRequest): Unit ={
    //check if topic exists
    
    if ((topicMapManager.tMap.containsKey(subscriptionMessage.subscriptionID))){
      //topic exist
      //createTopic(message.name, wsEvent);
      //authorize handshake
      subscriptionMessage.websockHandshake.authorize();
      
       val webSocketBroadcaster : ActorRef = topicMapManager.getActor(subscriptionMessage.subscriptionID); //topicMap.get(subscriptionMessage.subscriptionID);
       //subscribe handshake to topic
       webSocketBroadcaster ! new WebSocketBroadcasterRegistration(subscriptionMessage.websockHandshake);       
      
    } else {
      
     log.info("Topic does not exist")
    }    
    
  }

}