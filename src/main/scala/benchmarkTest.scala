import akka.actor.ActorSystem
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
import test.benchmarkDispatcher


object benchmarkTest {

  def storeManager = storage.storageManager;
  
  def main(args: Array[String]): Unit = {
    if (args.size < 1){
      
    println("Please select an option\n")
    println("1 Secure HTTP SPDY app")
    println("2 Standard secure app")
    println("3 Standard non secure app")    

    exit
      
    }
    
    if (args.size < 3){
      println("Enter: 'option' 'port' 'voldemort setup directory'")
      exit
    }
    
    val selection: Int = args(0).toInt;
    val port: Int = args(1).toInt;
    
    
 /*  if (selection == 3) {
      val tester = new benchmarkDispatcher;
      tester.start();
    }*/
    
    if (selection == 1 || selection == 2 || selection == 3){
      implicit val system = ActorSystem.create("NodeActorSystem", ConfigFactory.load().getConfig("AkkaConfig")); //ActorSystem("VoldertTestSystem");
    //implicit val system = ActorSystem("VoldertTestSystem");
    val journalDirectory: String = "target/EventSouredData";
    val journal: ActorRef = LeveldbJournalProps(new File(journalDirectory), native = false).createJournal;
    val extension: EventsourcingExtension = EventsourcingExtension(system, journal);
    
    
    val voldActor = system.actorOf(Props[voldCoordinator], name = "voldemorCordAtor");
    
    storeManager.startupSetup(args(2));
//actorOf(Props(new clientLogWebSocketEventFrame(messageRoutingActor)),"clientLogWebSocketActor");
//(extension : EventsourcingExtension, subSystemID: Int) 

    val id: Int = 0;
    //need to create configurations for these actors
    val messagingMasterActor = system.actorOf(Props (new masterMessagingActor(extension, id)), name = "messagingMasterActor");
    val routingWorkerMasterActor = system.actorOf(Props (new masterRouterActor(extension, id)), name = "masterRouterWorkerActor");
      
    val webServerActor = system.actorOf(Props(new dispatcher(selection, port, system, voldActor, messagingMasterActor, routingWorkerMasterActor)), name = "dispatcherActor");
      
    }
    
   
    
    
    
  }

}