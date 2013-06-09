package akka.router

import akka.actor.Actor
import akka.actor.ActorLogging
import messages.workerMessage
import java.util.HashMap
import java.util.Map
import akka.actor.ActorRef
import org.mashupbots.socko.events.WebSocketFrameEvent
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

class routeMessageActor extends Actor with ActorLogging {
  
  
  implicit val formats = DefaultFormats; // Brings in default date formats etc for JSON Lift
  
  def workerSocketFrameEventMap = workerMap.workSocketMap;
  
  def receive = {
    case workMessage: workerMessage => {
      //val workerActor: ActorRef = workerMap.get(workMessage.workerName);
      //send message to worker 
      //workerActor ! workMessage;
      
    }
    
    case _=> log.info("unknown message")
  }
  
  def sendMessageToWorker(wMessage: workerMessage): Unit ={
    //get socket for worker
    val workerName = wMessage.workerName;
    if (workerSocketFrameEventMap.workerSocketMap.containsKey(workerName)){      
      val workerWebSocket: WebSocketFrameEvent = workerSocketFrameEventMap.getWorkerWebSocket(workerName);
      
      //create JSON message
      val webSocketObject: Object = wMessage.websocketEvent.asInstanceOf[Object];
      //val newObject: Object = null;
      //val json = ("worker" -> workerName)~("operationData" -> wMessage.dataOperation)~("websocketObject" -> newObject);
      //val messageToSend: String = compact(render(json));
      
    } else {
      log.info("worker doesnt exist: " + workerName);
    }
    
  }

}