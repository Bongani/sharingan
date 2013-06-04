package akka.messaging

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.webserver.broadcastMessage
import akka.actor.ActorRef
import org.mashupbots.socko.handlers.WebSocketBroadcastText
import akka.actor.actorRef2Scala
import akka.messaging.topicMap
import akka.webserver.broadcastMessage


//can have multiple instances of this
class broadcasterManagerActor extends Actor with ActorLogging{
  
  def topicMapManager = topicMap.topicNameActorMap;

  def receive = {
    case message : broadcastMessage =>{
      //broadcast message recieved
      messageBroadcaster(message);
    }    
    case _=> log.info("unknown message")
  }
  
  def messageBroadcaster(bMessage : broadcastMessage): Unit = {
    
    //get topic
    if ((topicMapManager.tMap.containsKey(bMessage.topicID))){
      //topic exist
       val webSocketBroadcaster : ActorRef = topicMapManager.getActor(bMessage.topicID); //topicMap.get(subscriptionMessage.subscriptionID);
       val message = bMessage.websockEvent.readText
       webSocketBroadcaster ! WebSocketBroadcastText(message);//! new WebSocketBroadcasterRegistration(subscriptionMessage.websockHandshake);       
      
    } else {
      
     log.info("Topic does not exist")
    }  
    
    
    
    //if topic exists send message
    
    //store message
    
  }
  
}