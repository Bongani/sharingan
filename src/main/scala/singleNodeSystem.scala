/*import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import akka.actor.ActorRef
import akka.actor.Props
import akka.voldemort.voldactors.voldCoordinator
import configuration.storage
import akka.messaging.masterMessagingActor
import akka.router.masterRouterActor
import akka.webserver.dispatcher

import java.io.File
import org.eligosource.eventsourced.core._
import org.eligosource.eventsourced.journal.leveldb.LeveldbJournalProps

object singleNodeSystem {

  def storeManager = storage.storageManager;
  
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem.create("NodeActorSystem", ConfigFactory.load().getConfig("AkkaConfig")); //ActorSystem("VoldertTestSystem");
    //implicit val system = ActorSystem("VoldertTestSystem");
    val journalDirectory: String = "target/trial";
    val journal: ActorRef = LeveldbJournalProps(new File(journalDirectory), native = false).createJournal;
    val extension: EventsourcingExtension = EventsourcingExtension(system, journal);
    
    
    val voldActor = system.actorOf(Props[voldCoordinator], name = "voldemorCordAtor");
    
    storeManager.startupSetup(args(0));
//actorOf(Props(new clientLogWebSocketEventFrame(messageRoutingActor)),"clientLogWebSocketActor");
//(extension : EventsourcingExtension, subSystemID: Int) 

    val id: Int = 0;
    //need to create configurations for these actors
    val messagingMasterActor = system.actorOf(Props (new masterMessagingActor(extension, id)), name = "messagingMasterActor");
    val routingWorkerMasterActor = system.actorOf(Props (new masterRouterActor(extension, id)), name = "masterRouterWorkerActor");
      
    val webServerActor = system.actorOf(Props(new dispatcher(system, voldActor, messagingMasterActor, routingWorkerMasterActor)), name = "dispatcherActor");
    
  }

}*/