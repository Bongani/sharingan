package akka.router.adminActors

import akka.actor.Actor
import akka.actor.ActorLogging
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import org.mashupbots.socko.events.WebSocketFrameEvent
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.router.dipatcherMessage
import org.eligosource.eventsourced.core._

sealed trait workManagerEvents
case class adminMessage(operation: String, worker: String, workerWebSocket : WebSocketFrameEvent) extends workManagerEvents;

case class jsonWorkAdminMessage(operation: String, workerName: String);

class workerManagerActor (adminActor : ActorRef) extends Actor with ActorLogging  {
  
  
  implicit val formats = DefaultFormats; // Brings in default date formats etc for JSON Lift
  
  
  def receive = {
    case message: dipatcherMessage => {
      val workerAdminMessage: adminMessage = decodeAdminMessage(message.dataMessage, message.webSocket); 
      adminActor ! Message(workerAdminMessage);
    }
    
    case _=> log.info("unknown message")
  }
  
  
  def decodeAdminMessage(message : String, wsEvent: WebSocketFrameEvent): adminMessage = {
    val json = parse(message);      
    val jsonData = json.extract[jsonWorkAdminMessage];
    
    val taskOperation = jsonData.operation;
    val workerName = jsonData.workerName;
    
    val adminMessage = new adminMessage(taskOperation, workerName, wsEvent);
    return adminMessage;
    
  }
  
  override def preStart() {
    log.info("Starting workerManagerActor (workerManagerActor under routerDispatcherActor) instance hashcode # {}", this.hashCode());  
  }
  
  override def postStop() {
    log.info("Stopping workerManagerActor (workerManagerActor under routerDispatcherActor) instance hashcode # {}",this.hashCode());
  }
   

}