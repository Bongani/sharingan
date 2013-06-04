package akka.router

import akka.actor.Actor
import akka.actor.ActorLogging
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import org.mashupbots.socko.events.WebSocketFrameEvent
import akka.actor.Props

sealed trait workManagerEvents
case class adminMessage(operation: String, worker: String, workerWebSocket : WebSocketFrameEvent) extends workManagerEvents;

case class jsonWorkAdminMessage(operation: String, workerName: String);

class workerManagerActor extends Actor with ActorLogging  {
  
  
  implicit val formats = DefaultFormats; // Brings in default date formats etc for JSON Lift
  
  val adminActor = context.actorOf(Props[adminWorkerActor],"adminWorkerActor")

  
  def receive = {
    case websocketEvent: WebSocketFrameEvent => {
      val messageStringfied = websocketEvent.readText;
      val workerAdminMessage: adminMessage = decodeAdminMessage(messageStringfied, websocketEvent); 
      adminActor ! workerAdminMessage;
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
   

}