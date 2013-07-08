package akka.router

import akka.actor.ActorRef
import java.util.HashMap
import java.util.Map

object routerActorMap {
  val routerActorRefMap = new raMap;
}

class raMap {
  var actorMap: Map[String, ActorRef] = new HashMap[String, ActorRef];
  
  def getActor(actorName : String): ActorRef = {
    return actorMap.get(actorName);  
  }
  
  def putActor(actorName : String, routerActor : ActorRef): Unit ={
    actorMap.put(actorName, routerActor);
  }
  
  def removeActor(actorName : String): Unit ={
    actorMap.remove(actorName);
  }
  
}