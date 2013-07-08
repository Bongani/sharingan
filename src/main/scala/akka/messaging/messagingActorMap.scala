package akka.messaging

import akka.actor.ActorRef
import java.util.HashMap
import java.util.Map

object messagingActorMap {
  val messagingActorRefMap = new maMap;
}

class maMap {
  var actorMap: Map[String, ActorRef] = new HashMap[String, ActorRef];
  
  def getActor(actorName : String): ActorRef = {
    return actorMap.get(actorName);  
  }
  
  def putActor(actorName : String, messagingActor : ActorRef): Unit ={
    actorMap.put(actorName, messagingActor);
  }
  
  def removeActor(actorName : String): Unit ={
    actorMap.remove(actorName);
  }
  
}