package akka.router.routingActor

import akka.actor.Actor
import akka.actor.ActorLogging
import messages.workerMessage
import org.mashupbots.socko.events.WebSocketFrameEvent
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import messages.requestWorkerSocket
import akka.actor.ActorRef
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.Await
import scala.util.{Success, Failure}
import scala.concurrent.TimeoutException
import akka.dispatch.Futures
import akka.dispatch.OnComplete
import scala.concurrent.ExecutionContext.Implicits.global


class routeMessageActor (adminWorkerActor : ActorRef) extends Actor with ActorLogging {
  
  implicit var timeout = Timeout(5 seconds);
  
  implicit val formats = DefaultFormats; // Brings in default date formats etc for JSON Lift
  
  //def workerSocketFrameEventMap = workerMap.workSocketMap;
  
  def receive = {
    case workMessage: workerMessage => {
      sendMessageToWorker(workMessage);      
    }
    
    case _=> log.info("unknown message")
  }
  
  def sendMessageToWorker(wMessage: workerMessage): Unit ={
    //get socket for worker
    val workerName = wMessage.workerName;
    
    val msg = new requestWorkerSocket(workerName);
    adminWorkerActor ? msg onComplete {
      case Success(workerWebSocket : WebSocketFrameEvent) => {
              if (workerWebSocket != null){
                 //create JSON message
                // println("\n \n Failure  here \n \n");
                val clientChannel : Int = wMessage.websocketEvent.channel.getId();
                //println()
                val json = ("worker" -> workerName)~("operationData" -> wMessage.dataOperation)~("clientChannel" -> clientChannel)~("response" -> wMessage.expectingResponse);
                val messageToSend: String = compact(render(json));
                //println(messageToSend);
                //write message to websocket
                workerWebSocket.writeText(messageToSend);
                
                
              } else {
                log.info("worker doesnt exist: " + workerName);
              }           
            }
            case Failure(e: TimeoutException) => {
              log.info("failed to retrieve client channel");

            }
          }
    
    
    
   /* if (workerSocketFrameEventMap.workerSocketMap.containsKey(workerName)){      
      val workerWebSocket: WebSocketFrameEvent = workerSocketFrameEventMap.getWorkerWebSocket(workerName);
      
      //create JSON message
     // println("\n \n Failure  here \n \n");
      val clientChannel : Int = wMessage.websocketEvent.channel.getId();
      //println()
      val json = ("worker" -> workerName)~("operationData" -> wMessage.dataOperation)~("clientChannel" -> clientChannel)~("response" -> wMessage.expectingResponse);
      val messageToSend: String = compact(render(json));
      println(messageToSend);
      //write message to websocket
      workerWebSocket.writeText(messageToSend);
      
    } else {
      log.info("worker doesnt exist: " + workerName);
    }*/
    
  }

}