package akka.node

import akka.webserver.superNodeWebSocketFrame
import akka.actor.ActorLogging
import akka.actor.Actor
import akka.webserver.superNodeWebSocketFrameStorage
import akka.actor.ActorRef
import akka.webserver.actorRequest
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.Await
import scala.util.{Success, Failure}
import scala.concurrent.TimeoutException
import akka.dispatch.Futures
import akka.dispatch.OnComplete
import scala.concurrent.ExecutionContext.Implicits.global
import akka.webserver.superNodeWebRequestWithPath
import akka.webserver.subscriptionRequest
import akka.webserver.superNodeWebBroadcastWithPath
import akka.webserver.broadcastMessage

//can have multiple instances
class dispatherToActor extends Actor with ActorLogging{
  implicit var timeout = Timeout(5 seconds);
  
  def receive = {
    case message: superNodeWebSocketFrameStorage => {
      sendMessageToStorageActor(message);
    }
    
    case message: superNodeWebSocketFrame => {
      sendMessageToActor(message);
    }   
    
    case message: superNodeWebRequestWithPath => {
      sendMessageToSubscriptionManager(message);
    } 
    
    case message: superNodeWebBroadcastWithPath => {
      sendMessageToBroadcastActor(message);
    } 
    case _=> log.info("unknown message")
  }
  
  def sendMessageToStorageActor(message: superNodeWebSocketFrameStorage): Unit ={
    val node: systemNode = message.node;
    node.voldActor ! message.wsFRame
    
  }
  
  def sendMessageToActor(message : superNodeWebSocketFrame): Unit ={
    val node: systemNode = message.node;
    var subSystemMasterActor : ActorRef = null;
        
    if (message.subSystemName == "messaging"){
      subSystemMasterActor = node.messagingMasterActor; 
    } else if (message.subSystemName == "router"){
      subSystemMasterActor = node.routingWorkerMasterActor;
    }
    
    val actorRequestMsg = new actorRequest(message.actorName);
    subSystemMasterActor ? actorRequestMsg onComplete {
      case Success(retrievedActor  : ActorRef) => {
        if (retrievedActor != null){
          retrievedActor ! message.wsFRame
                
              } else {
                log.info("Actor does not exist");
              }           
            }
            case Failure(e: TimeoutException) => {
              log.info("failed to retrieve actor");

            }
          }
    
  }
  
  def sendMessageToSubscriptionManager(message : superNodeWebRequestWithPath): Unit ={
    val node: systemNode = message.node;
    var subSystemMasterActor : ActorRef = node.messagingMasterActor;
    
    val actorRequestMsg = new actorRequest(message.actorName);
    subSystemMasterActor ? actorRequestMsg onComplete {
      case Success(retrievedActor  : ActorRef) => {
        if (retrievedActor != null){
          val request = new subscriptionRequest(message.wsHandshake, message.path);
          retrievedActor ! request
                
              } else {
                log.info("Actor does not exist");
              }           
            }
            case Failure(e: TimeoutException) => {
              log.info("failed to retrieve actor");

            }
          }
    
  }
  
  def sendMessageToBroadcastActor(message : superNodeWebBroadcastWithPath): Unit ={
    val node: systemNode = message.node;
    var subSystemMasterActor : ActorRef = node.messagingMasterActor;
    
    val actorRequestMsg = new actorRequest(message.actorName);
    subSystemMasterActor ? actorRequestMsg onComplete {
      case Success(retrievedActor  : ActorRef) => {
        if (retrievedActor != null){
          val broadcast = new broadcastMessage(message.wsFRame, message.path);
          retrievedActor ! broadcast
                
              } else {
                log.info("Actor does not exist");
              }           
            }
            case Failure(e: TimeoutException) => {
              log.info("failed to retrieve actor");

            }
          }
    
  }
  
   override def preStart() {
    log.info("Starting dispatherToActor (dispatherToActor under node system) instance hashcode # {}", this.hashCode());  
  }
  
  override def postStop() {
    log.info("Stopping dispatherToActor (dispatherToActor under node system) instance hashcode # {}",this.hashCode());
  } 
  
    

}