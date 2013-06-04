package akka.messaging

import java.util.HashMap
import java.util.Map
import akka.actor.ActorRef

object topicMap {
  
  val topicNameActorMap = new map;

}

class map {
  var tMap: Map[String, ActorRef] = new HashMap[String, ActorRef];
  
  def getActor(topicName : String): ActorRef = {
    return tMap.get(topicName);  
  }
  
  def putActor(topicName : String, broadcastActor : ActorRef): Unit ={
    tMap.put(topicName, broadcastActor);
  }
  
  def removeActor(topicName : String): Unit ={
    tMap.remove(topicName);
  }
}