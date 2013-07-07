package akka.voldemort.voldactors.actionactors

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.voldemort.voldactors.put
import akka.voldemort.voldactors.putVersion


class putActor extends Actor with ActorLogging{
  
  def receive = {
    case message: put => {
      message.clientStore.put(message.key, message.value);
    }
    case message: putVersion => {
      message.clientStore.put(message.key, message.versioned);
    }
    case _ => log.info("putActor recieved an unknown message");
  }
  
  override def preStart() {
    log.info("Starting putActor (putActor under voldCoordinator) instance hashcode # {}", this.hashCode());  
  }
  
  override def postStop() {
    log.info("Stopping putActor (putActor under voldCoordinator) instance hashcode # {}",this.hashCode());
  }  
  
}