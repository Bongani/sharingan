package akka.router

import akka.actor.Actor
import akka.actor.ActorLogging
import messages.workerMessage
import java.util.HashMap
import java.util.Map
import akka.actor.ActorRef

class routeMessageActor extends Actor with ActorLogging {
  
  //var workerMap: Map[String, ActorRef] = new HashMap[String, ActorRef];
  
  def receive = {
    case workMessage: workerMessage => {
      //val workerActor: ActorRef = workerMap.get(workMessage.workerName);
      //send message to worker 
      //workerActor ! workMessage;
    }
    
    case _=> log.info("unknown message")
  }

}