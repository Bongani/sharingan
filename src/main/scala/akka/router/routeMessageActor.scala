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
      sendMessageToWorker(workMessage);      
    }
    
    case _=> log.info("unknown message")
  }
  
  def sendMessageToWorker(wMessage: workerMessage): Unit ={
    //get socket for worker
    val workerName = wMessage.workerName;
    if (workerSocketFrameEventMap.workerSocketMap.containsKey(workerName)){      
      val workerWebSocket: WebSocketFrameEvent = workerSocketFrameEventMap.getWorkerWebSocket(workerName);
      
      //create JSON message
      val clientChannel : String = wMessage.websocketEvent.channel.getId().asInstanceOf[String];      
      val json = ("worker" -> workerName)~("operationData" -> wMessage.dataOperation)~("clientChannel" -> clientChannel)~("response" -> wMessage.expectingResponse);
      val messageToSend: String = compact(render(json));
      
      //write message to websocket
      workerWebSocket.writeText(messageToSend);
      
    } else {
      log.info("worker doesnt exist: " + workerName);
    }
    
  }

}