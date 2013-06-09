package akka.voldemort.voldactors.actionactors

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.voldemort.voldactors.put
import akka.voldemort.voldactors.putVersion


/*trait putInterface {
  
  def put(key: Object, value: Object, clientStore: DefaultStoreClient[Object, Object]): Unit;
  //clientStore is a voldemort client for a specific store
  
  //Put the given Versioned value into the store for the given key if the version is greater to or concurrent with existing values.
  def putVersion(key: Object, versioned: Versioned[Object] , clientStore: DefaultStoreClient[Object, Object]): Unit;
}

class putActor extends putInterface {
  
  //fire and forget
  def put(key: Object, value: Object, clientStore: DefaultStoreClient[Object, Object]): Unit = {
    clientStore.put(key, value);
  }
  //will need to handle exceptions like if voldemort cluster is no longer there
  
  def putVersion(key: Object, versioned: Versioned[Object] , clientStore: DefaultStoreClient[Object, Object]): Unit = {
    clientStore.put(key, versioned);
  }
  

}*/

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