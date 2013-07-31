package akka.messaging

import akka.actor.Actor
import akka.actor.ActorLogging
import messages.topicMessage
import org.mashupbots.socko.events.WebSocketFrameEvent
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import akka.actor.Props
import org.mashupbots.socko.handlers.WebSocketBroadcaster
import org.eligosource.eventsourced.core._
import java.util.HashMap
import java.util.Map
import akka.actor.ActorRef
import messages.requestTopicBroadcastcActor

class topicManagementWorkActor(extension : EventsourcingExtension, processorID : Int) extends Actor with ActorLogging {
  
  implicit val formats = DefaultFormats; // Brings in default date formats etc for JSON Lift
  
  //def topicMapManager = topicMap.topicNameActorMap;
  var topicMapManager: Map[String, ActorRef] = new HashMap[String, ActorRef];
  
  def receive = {
    case evtSourcedMessage: Message => {
      //topicMessage
      val message: topicMessage = evtSourcedMessage.event.asInstanceOf[topicMessage];
      val operation = message.messageTask;
      operation match {
        case "create" => {
          newTopic(message);
          }
        case "remove" => {}
        case _=> {
          log.info("Recieved unknown operation message for topic admin actor");
          //Fire back succesful creation of topic
          val json = ("topicName" -> message.name)~("task" -> "failure");
          val returnMessage: String = compact(render(json));
          message.webSocketEvent.writeText(returnMessage);
          }
      }      
    }
    
    case topicActorRequest: requestTopicBroadcastcActor => {
      val webSocketBroadcasterActor : ActorRef = topicMapManager.get(topicActorRequest.name);
      sender ! webSocketBroadcasterActor;
    }
    
    case sr @ SnapshotRequest(pid, snr, _) => {
        sr.process(topicMapManager)
        println(s"processed snapshot request for (snr = ${snr})")

      }
    case so @ SnapshotOffer(Snapshot(_, snr, time, tMap: Map[String, ActorRef])) => {
        topicMapManager = tMap
        println(s"accepted snapshot offer for (snr = ${snr} time = ${time}})")
      }
        
    case _=> log.info("unknown message")
  }
  
  
  def newTopic(message: topicMessage): Unit ={
    
    //check if topic exists
    if (!(topicMapManager.containsKey(message.name))){
      //topic does not exist
      createTopic(message.name, message.webSocketEvent);
      log.info("new topic created: " + message.name);
      //need to fire back that topic has been created
    } else {
      //topic exists
      log.info("new topic already exists: " + message.name);
      //need to fire back that topic has been created
      //Fire back succesful creation of topic
      val json = ("topicName" -> message.name)~("task" -> "success");
      val returnMessage: String = compact(render(json));
      message.webSocketEvent.writeText(returnMessage);
     
    }
    
  }
  
  
    //must verify topic does not exist before creating it
  def createTopic(topicName: String, event: WebSocketFrameEvent): Unit ={
    val actorBroadcasterName: String = "webSocketBroadcaster" + topicName;
    
    //create broadcast actor. Is child of topicManagerActor
    val webSocketBroadcaster = context.actorOf(Props[WebSocketBroadcaster], actorBroadcasterName);
    
    //topicMap.put(topicName, webSocketBroadcaster);
    topicMapManager.put(topicName, webSocketBroadcaster);
    
    //Fire back succesful creation of topic
    val json = ("topicName" -> topicName)~("task" -> "success");
    val returnMessage: String = compact(render(json));
    event.writeText(returnMessage);
  }
  
  override def preStart() {
    log.info("Starting topicManagementWorkActor (topicManagementWorkActor under masterMessagingActor) instance hashcode # {}", this.hashCode());  
  }
  
  override def postStop() {
    log.info("Stopping topicManagementWorkActor (topicManagementWorkActor under masterMessagingActor) instance hashcode # {}",this.hashCode());
  }
  
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
//need to overide with empty method so eventsourced recovery works
    }
  
  override def postRestart(reason: Throwable): Unit = {
    println("\n recovering messages for actor crashing\n")
    extension.recover(Seq(ReplayParams(processorID, snapshot = true)))
  }

}