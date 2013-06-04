package messaging

import akka.actor.Actor
import akka.actor.ActorLogging
import java.util.Map
import java.util.HashMap
import messages.subscriptionMessage
import voldemort.client.ClientConfig
import voldemort.client.DefaultStoreClient
import voldemort.client.StoreClientFactory
import voldemort.client.protocol.RequestFormatType
import voldemort.client.SocketStoreClientFactory
import messages.hostDetails
import messages.voldemortMessage
import messages.topicMessage
import akka.actor.ActorRef
import messages.voldemortDirectMessage


class topicManager(storageActor: ActorRef) extends Actor with ActorLogging {
  
  var messageStoreClient: DefaultStoreClient[Object, Object] = null;
  //var subsriberSet: Set[hostDetails] = new HashSet[hostDetails];
  //var subscriberMap: Map[String, Integer] = new HashMap[String, Integer];
  var topicMap: Map[String, Map[String, Integer]] = new HashMap[String, Map[String, Integer]];
  
  override def preStart(){
    //setup topic manager
    
    //have to create actors subscriber
    
    //setup voldemort connection
    val bootstrapUrl = "tcp://localhost:6666"; //assume for now voldemort is on the same host for now
    val storeName = "messaging";
    
    val clientConfig: ClientConfig = new ClientConfig().setBootstrapUrls(bootstrapUrl).setEnableLazy(false).setRequestFormatType(RequestFormatType.VOLDEMORT_V3);
    
    try {
      val factory: StoreClientFactory = new SocketStoreClientFactory(clientConfig);
      messageStoreClient = factory.getStoreClient(storeName).asInstanceOf[DefaultStoreClient[Object, Object]];
      // adminClient = new AdminClient(bootstrapUrl, new AdminClientConfig());
    } catch{
       case e: Exception => println("Could not connect to server: " + e.getMessage());
    }
    
  }
  
  override def postStop(){
    
  }
  
  def receive = {
    case topicMsg: topicMessage =>{
      val operation = topicMsg.messageTask;
      operation match {
        case "create" => createTopic(topicMsg.name);
        case "delete" => deleteTopic(topicMsg.name);
      }
    }
    
    case subscripMessage: subscriptionMessage => {
      val operation = subscripMessage.messageTask;
      operation match {
        case "subscribe" => subscribe(subscripMessage.messageHost);
        case "unsubscribe" => unsubscribe(subscripMessage.messageHost);
        case "modify" => modifySubscription(subscripMessage.messageHost);
      }
      
    }
    
    case _=> log.info("unknown message")
  }
  
  def createTopic(topic: String) : Unit ={
    val subscribersOfTopic: Map[String, Integer] = new HashMap[String, Integer];
    topicMap.put(topic, subscribersOfTopic);
    
    val vMessage = new voldemortDirectMessage("put",messageStoreClient, topic, subscribersOfTopic,null,null,null);
    //fire message
    storageActor ! vMessage;
    
  }
  
  def deleteTopic(topic: String) : Unit ={
    topicMap.remove(topic);
    
    val vMessage = new voldemortDirectMessage("delete",messageStoreClient, topic, null,null,null,null);
    //fire message
    storageActor ! vMessage;
  }
  
  def subscribe(host: hostDetails) : Unit ={
    val topic: String = host.topicName;
    val subscribersOfTopic: Map[String, Integer] = topicMap.get(topic);
    
    val key: String = host.name + host.ipAdd.toString();
    val value: Integer = host.topicRank;
    subscribersOfTopic.put(key, value);
    
    //key for voldemort storage is topic name, value is map
    val vMessage = new voldemortDirectMessage("put",messageStoreClient, topic, subscribersOfTopic,null,null,null);
    //firing message
    storageActor ! vMessage;
  }
  
  def unsubscribe(host: hostDetails) : Unit ={
    val topic: String = host.topicName;
    val subscribersOfTopic: Map[String, Integer] = topicMap.get(topic);
    
    val key: String = host.name + host.ipAdd.toString();
    subscribersOfTopic.remove(key);
    
     //key for voldemort storage is topic name, value is map
    val vMessage = new voldemortDirectMessage("put",messageStoreClient, topic, subscribersOfTopic,null,null,null);
    //fire message
    storageActor ! vMessage;
    
  }
  
  def modifySubscription(host: hostDetails) : Unit = {
    val topic: String = host.topicName;
    val subscribersOfTopic: Map[String, Integer] = topicMap.get(topic);
    
    val key: String = host.name + host.ipAdd.toString();
    val value: Integer = host.topicRank;
     subscribersOfTopic.put(key, value);
    
    //key for voldemort storage is topic name, value is map
    val vMessage = new voldemortDirectMessage("put",messageStoreClient, topic, subscribersOfTopic,null,null,null);
    //fire message
    storageActor ! vMessage;
  }

}

