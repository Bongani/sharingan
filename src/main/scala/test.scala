/*
import java.io.File
import akka.actor._
import org.eligosource.eventsourced.core._
import org.eligosource.eventsourced.journal.leveldb.LeveldbJournalProps
import akka.voldemort.voldactors.voldCoordinator


object test {
  

  
  def main(args: Array[String]): Unit = {
    
    implicit val system = ActorSystem("test");
    
    val journalDirectory: String = "target/trial";
    println("Starting new operation test")
    //val journal: ActorRef = LeveldbJournalProps(new File(journalDirectory), native = false).createJournal;
    //val extension: EventsourcingExtension = EventsourcingExtension(system, journal);
    
    
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
