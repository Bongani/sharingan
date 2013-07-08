package akka.node

import akka.actor.ActorLogging
import akka.actor.Actor
import org.mashupbots.socko.events.WebSocketFrameEvent
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import akka.actor.ActorRef

sealed trait adminNodeEvent
case class nodeManagerMessage(operation: String, nodeID: String) extends adminNodeEvent;


case class jsonNodeManagerMessage(operation: String, nodeID: String);

class jsonDecoder(nodeManager : ActorRef) extends Actor with ActorLogging {
  
  implicit val formats = DefaultFormats; // Brings in default date formats etc for JSON Lift
  
  def receive = {
    case websocketEvent: WebSocketFrameEvent => {
      log.info("recieved JSON message for Node Admin")
      val jMessageStringfied = websocketEvent.readText;
      val managerMessage : nodeManagerMessage = decodeJSONMessage(jMessageStringfied);
      nodeManager ! managerMessage;
    }
    
    case _=> log.info("unknown message")
  }
  
  def decodeJSONMessage(message : String): nodeManagerMessage = {
    val json = parse(message);      
    val jsonData = json.extract[jsonNodeManagerMessage];
    
    val adminOperation = jsonData.operation;
    val nodeIdentifier = jsonData.nodeID;
    
    val adminMessage = new nodeManagerMessage(adminOperation, nodeIdentifier);
    return adminMessage;
    
  }

}