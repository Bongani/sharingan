package akka.voldemort.voldactors.actionactors

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.voldemort.voldactors.get
import akka.voldemort.voldactors.getVersioned
import voldemort.versioning.Versioned
import java.io.IOException


/*trait getInterface{
  //get value only
  def get(key: Object, clientStore: DefaultStoreClient[Object, Object]): Option[Versioned[Object]];
  
  //Get the versioned value associated with the given key or the defaultValue if no value is associated with the key
  def getVersioned(key: Object, defaultValue: Versioned[Object], clientStore: DefaultStoreClient[Object, Object]): Option[Versioned[Object]];
  
  //Returns the list of nodes which should have this key. Returns: java.util.List<Node>
  //def getResponsibleNodes(key: Object, clientStore: DefaultStoreClient[Object, Object]): Option[java.util.List[Node]]
  
}


class getActor extends getInterface {
  
  def get(key: Object, clientStore: DefaultStoreClient[Object, Object]): Option[Versioned[Object]] = {
    val value = clientStore.get(key);
    return Some(value);
  }
  
  def getVersioned(key: Object, defaultValue: Versioned[Object], clientStore: DefaultStoreClient[Object, Object]): Option[Versioned[Object]] = {
    val value = clientStore.get(key, defaultValue);
    return Some(value);
  }

}*/

class getActor extends Actor with ActorLogging{
  
  def receive = {
    case message: String => throw new IOException
    case message: get => {
      val value: Versioned[Object] = message.clientStore.get(message.key);
      sender ! value;      
    }
    case message: getVersioned => {
      val value: Versioned[Object] = message.clientStore.get(message.key);
      sender ! value;
    }
    case _=> log.info("unknown message")
  }
  
  override def preStart() {
    log.info("Starting getActor (getActor under voldCoordinator) instance hashcode # {}", this.hashCode());  
  }
  
  override def postStop() {
    log.info("Stopping getActor (getActor under voldCoordinator) instance hashcode # {}",this.hashCode());
  }
  
}