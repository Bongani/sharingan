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
import akka.routing.SmallestMailboxRouter
import akka.routing.DefaultResizer
import akka.routing.FromConfig
import scala.xml.XML


object benchmarkTest {
  
  var actorLower : Int = 1;
  var actorUpper : Int = 2;

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
    
    
    //val voldActor = system.actorOf(Props[voldCoordinator], name = "voldemorCordAtor");
    actorConfig();
    val resizer = new DefaultResizer(lowerBound = actorLower, upperBound = actorUpper);
 
    
    
    val voldActor = system.actorOf(Props[voldCoordinator].withRouter(SmallestMailboxRouter(resizer = Some(resizer))), name = "voldemorCordAtor");
    
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
  
  def actorConfig() : Unit = {
    var workingDirectory = new java.io.File(".").getCanonicalPath();
    var folderPath: String = workingDirectory + "/config/Actors/";
    var configFolder = new File(folderPath);
    
    if (configFolder.exists()){
      //store actor information
      var storageXML : scala.xml.Elem = readXMLFile(folderPath, "storage.xml");
      //readStoreContent(storageXML);
      
      if (storageXML != null){
              
        val actorLowerBound = (storageXML \"storageCoor"\ "lowerBound").text;
        val actorUpperBound = (storageXML \"storageCoor"\ "upperBound").text;
        actorLower = actorLowerBound.toInt;
        actorUpper = actorUpperBound.toInt;
      }
      
      
    } else {
      println(" \n \n" + "Error: The folder for storage sctors could not be found. Path given: " + configFolder + " \n \n");
      println("Will use generic settings")
     
    }
  }
  
  
  def readXMLFile(path: String, fileName: String): scala.xml.Elem ={
    
    var xmlFile = new File(path, fileName); 
    if (xmlFile.exists()){
      var xmlContent = XML.loadFile(xmlFile);
      println("Read XML file for: " + fileName);
      return xmlContent;
    } else {
      println("File not found for XML reading: " + fileName);
      return null;
    }
    
  }

}