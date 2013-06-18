import akka.actor.ActorSystem
import akka.actor.Props
import akka.voldemort.voldactors.voldCoordinator
import voldemort.client.ClientConfig
import voldemort.client.protocol.RequestFormatType
import voldemort.client.SocketStoreClientFactory
import voldemort.client.StoreClientFactory
import voldemort.client.DefaultStoreClient
import messages.voldemortMessage
import messages.voldemortDirectMessage
import akka.webserver.dispatcher
import configuration.storageManagement
import configuration.storage
import com.typesafe.config.ConfigFactory
import akka.routing.FromConfig
import akka.voldemort.voldactors.actionactors.deleteActor


object voldemortTest {
  
	def storeManager = storage.storageManager;

  
  def main(args: Array[String]): Unit = {
    
    val system = ActorSystem.create("VoldertTestSystem", ConfigFactory.load().getConfig("AkkaConfig")); //ActorSystem("VoldertTestSystem");
    val voldActor = system.actorOf(Props[voldCoordinator], name = "voldemorCordActor");
    
    storeManager.startupSetup(args(0));


    //need to create configurations for these actors
    val messagingMasterActor = system.actorOf(Props[masterMessagingActor], name = "messagingMasterActor");
    val routingWorkerMasterActor = system.actorOf(Props[masterRouterActor], name = "masterRouterWorkerActor");
  
    
  val webServerActor = system.actorOf(Props(new dispatcher(system, voldActor)), name = "dispatcherActor");
    
    
  }

}