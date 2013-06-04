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
    //val newActor = system.actorOf(Props[deleteActor].withRouter(FromConfig()), name = "deleteActor");
    
    
    //voldemort config
    /*val bootstrapUrl = "tcp://localhost:6666";
    val storeName = "test";
    
    val clientConfig: ClientConfig = new ClientConfig().setBootstrapUrls(bootstrapUrl).setEnableLazy(false).setRequestFormatType(RequestFormatType.VOLDEMORT_V3);
    var client: DefaultStoreClient[Object, Object] = null;
    try {
      val factory: StoreClientFactory = new SocketStoreClientFactory(clientConfig);
      client = factory.getStoreClient(storeName).asInstanceOf[DefaultStoreClient[Object, Object]];
      // adminClient = new AdminClient(bootstrapUrl, new AdminClientConfig());
    } catch{
       case e: Exception => println("Could not connect to server: " + e.getMessage());
    }
    
    //val v = new voldemortMessage("put",client, "hi", "Hey this is test",null,null,null);
    val v = new voldemortDirectMessage("get",client, "hi", "Hey this is test",null,null,null);
    myActor ! v;*/
    //System.exit(0);
   // val testWebActor = system.actorOf(Props[websocketHandler], name = "testActorWeb");
   // val webServerActor = system.actorOf(Props(new dispatcher(system, myActor)), name = "dispatcherActor");
    
    
   
   storeManager.startupSetup(args(0));
   //val tip = "hi";
   
  println(storeManager.getStorage("test"));
  //println(tip.getClass().getName())
  //val store2 = storageManagement();
  //println(store2.getStorage("test"));
 // println(" ")
  
    
  val webServerActor = system.actorOf(Props(new dispatcher(system, voldActor)), name = "dispatcherActor");
    
    
  }

}