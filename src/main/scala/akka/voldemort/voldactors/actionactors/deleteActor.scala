package akka.voldemort.voldactors.actionactors

import akka.actor.Actor
import akka.voldemort.voldactors.delete
import akka.voldemort.voldactors.deleteVersion
import akka.actor.ActorLogging

/*trait deleteInterface {
  
  //Delete any version of the given key which equal to or less than the current versions
  def delete(key: Object, clientStore: DefaultStoreClient[Object, Object]): Unit;
  //clientStore is a voldemort client for a specific store
  
  //Delete the specified version and any prior versions of the given key
  def deleteVersion(key: Object, version: Version, clientStore: DefaultStoreClient[Object, Object]): Unit;
}


class deleteActor extends deleteInterface{
  //fire and forget
  def delete(key: Object, clientStore: DefaultStoreClient[Object, Object]): Unit = {
    clientStore.delete(key);
  }
  
  //fire and forget
  def deleteVersion(key: Object, version: Version, clientStore: DefaultStoreClient[Object, Object]): Unit = {
    clientStore.delete(key,version);
  }
  
  //will need to handle exceptions like if voldemort cluster is no longer there
  //or the data you wish to delete is not there

}*/

class deleteActor extends Actor with ActorLogging{
  
  def receive = {
    case message: delete => {
      message.clientStore.delete(message.key);      
    }
    case message: deleteVersion => {
      message.clientStore.delete(message.key,message.version);
    }
    case _=> log.info("unknown message")
    
  }
  
  override def preStart() {
    log.info("Starting deleteActor (deleteActor under voldCoordinator) instance hashcode # {}", this.hashCode());  
  }
  
  override def postStop() {
    log.info("Stopping deleteActor (deleteActor under voldCoordinator) instance hashcode # {}",this.hashCode());
  }
  
}