package akka.messaging

import akka.actor.Actor
import akka.actor.ActorLogging
import messages.topicMessage
import org.mashupbots.socko.events.WebSocketFrameEvent
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import akka.actor.Props
import org.mashupbots.socko.handlers.WebSocketBroadcaster

class topicManagementWorkActor extends Actor with ActorLogging {
  
  implicit val formats = DefaultFormats; // Brings in default date formats etc for JSON Lift
  
  //def topicMapManager = topicMap.topicNameActorMap;
  
  def receive = {
    case message: topicMessage => {
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
        
    case _=> log.info("unknown message")
  }
  
  
  def newTopic(message: topicMessage): Unit ={
    
    //check if topic exists
    if (!(topicMapManager.tMap.containsKey(message.name))){
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
    topicMapManager.putActor(topicName, webSocketBroadcaster);
    
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

}