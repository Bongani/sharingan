package akka.messaging

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorLogging
import org.mashupbots.socko.events.WebSocketFrameEvent
import messages.topicMessage
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
//import akka.messaging.topicMap
import org.eligosource.eventsourced.core._

case class jsonTopicMessage(topicName: String, task: String);

class topicManagerActor ( tAdminActor: ActorRef) extends Actor with ActorLogging {
  //storageActor: ActorRef,
  implicit val formats = DefaultFormats; // Brings in default date formats etc for JSON Lift
  
  //var topicMap: Map[String, ActorRef] = new HashMap[String, ActorRef];
  //val tAdminActor = context.actorOf(Props[MyWorkerActor],"topicAdminActor")

  
  def receive = {
    case websocketEvent: WebSocketFrameEvent => {
      log.info("recieved JSON message for topic Management")
      val vMessageStringfied = websocketEvent.readText;
      val tMessage: topicMessage = decodeTopicMessage(vMessageStringfied, websocketEvent); 
      tAdminActor ! Message(tMessage);
      //need to tell admin actor to take a snapshot
      tAdminActor ! SnapshotRequest;
    }
    
    case _=> log.info("unknown message")
  }
  
  
  def decodeTopicMessage(message : String, wsEvent : WebSocketFrameEvent): topicMessage = {
    val json = parse(message);      
    val jsonData = json.extract[jsonTopicMessage];
    
    val topic = jsonData.topicName;
    val taskOperation = jsonData.task;
    
    val tMessage = new topicMessage(topic, taskOperation, wsEvent);
    return tMessage;
    
  }
  
  override def preStart() {
    log.info("Starting topicManagerActor (topicManagerActor under masterMessagingActor) instance hashcode # {}", this.hashCode());  
  }
  
  override def postStop() {
    log.info("Stopping topicManagerActor (topicManagerActor under masterMessagingActor) instance hashcode # {}",this.hashCode());
  }

}