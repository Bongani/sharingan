/*import configuration.storage
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import java.io.File
import org.eligosource.eventsourced.core._
import org.eligosource.eventsourced.journal.leveldb.LeveldbJournalProps
import akka.actor.ActorRef
import akka.actor.Props
import akka.node.nodeManager
import akka.routing.RoundRobinRouter
import akka.routing.DefaultResizer
import akka.node.dispatherToActor
import akka.webserver.superNodeDispatcher

object superNodeSystem {

  def storeManager = storage.storageManager;
  
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem.create("VoldertTestSystem", ConfigFactory.load().getConfig("AkkaConfig")); //ActorSystem("VoldertTestSystem");
    //implicit val system = ActorSystem("VoldertTestSystem");
    val journalDirectory: String = "target/trial";
    val journal: ActorRef = LeveldbJournalProps(new File(journalDirectory), native = false).createJournal;
    val extension: EventsourcingExtension = EventsourcingExtension(system, journal);
    
    
    storeManager.startupSetup(args(0));
    
    val processorID: Int = 1;
    val nodeManagerActor = extension.processorOf(Props(new nodeManager(system, extension) with Eventsourced { val id = processorID} ))
    nodeManagerActor ! "configNodes";
    
    //need to create configuration for this
    var actorLower : Int = 2;
    var actorUpper : Int = 15;
    val resizer = new DefaultResizer(lowerBound = actorLower, upperBound = actorUpper);
    val superNodeSubSystemActorDispatcher = system.actorOf(Props[dispatherToActor].withRouter(RoundRobinRouter(resizer = Some(resizer))), name = "subSystemActorDispatcher");
    
    val superNodeWebServerActor = system.actorOf(Props(new superNodeDispatcher(system, nodeManagerActor, superNodeSubSystemActorDispatcher)), name = "superNodeDispatcherActor");
    
  }

}*/