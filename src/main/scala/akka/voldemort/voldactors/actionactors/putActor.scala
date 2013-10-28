package akka.voldemort.voldactors.actionactors

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.voldemort.voldactors.put
import akka.voldemort.voldactors.putVersion
import akka.persistence.{ Persistent, PersistenceFailure, Processor }
import akka.persistence.Recover


class putActor extends Processor with ActorLogging{
  
  def receive = {
    case message: put => {
      message.clientStore.put(message.key, message.value);
    }
    case message: putVersion => {
      message.clientStore.put(message.key, message.versioned);
    }
    
    case Persistent(payload, sequenceNr) ⇒ {
      // message successfully written to journal
      val message:put = payload.asInstanceOf[put];
      message.clientStore.put(message.key, message.value);
    }
    case PersistenceFailure(payload, sequenceNr, cause) ⇒ {
      // message failed to be written to journal
      val message:put = payload.asInstanceOf[put];
      message.clientStore.put(message.key, message.value);
    }  
    
    
    case _ => log.info("putActor recieved an unknown message");
  }
  
  override def preStart() {
    log.info("ReStarting putActor (putActor under voldCoordinator) instance hashcode # {}", this.hashCode());
    self ! Recover()
    log.info("Recovering putActor (putActor under voldCoordinator) instance hashcode # {}", this.hashCode());
  }
  
  override def postStop() {
    log.info("Stopping putActor (putActor under voldCoordinator) instance hashcode # {}",this.hashCode());
  }  
  
}