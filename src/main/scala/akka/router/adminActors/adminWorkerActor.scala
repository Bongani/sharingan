package akka.router.adminActors

import akka.actor.Actor
import akka.actor.ActorLogging
import org.mashupbots.socko.events.WebSocketFrameEvent
import akka.router.workerMap

class adminWorkerActor extends Actor with ActorLogging{
  
  def workerSocketFrameEventMap = workerMap.workSocketMap;
  
  def receive = {
    case message : adminMessage => {
      val operation = message.operation;
      operation match {
        case "add" => {
          addWorker(message.worker, message.workerWebSocket);
        }
        case "delete" => {
          deleteWorker(message.worker);
        }
        case _=> log.info("Recieved unknown direct operation message for adminWorkerActor");
      }
    }
    case _=> log.info("unknown message")
  }
  
    
  def addWorker(workerName: String, webSocket: WebSocketFrameEvent): Unit ={
    workerSocketFrameEventMap.putWorkerWebSocket(workerName, webSocket);
    //need to fire back that worker was added
  }
  
  def deleteWorker(workerName: String): Unit ={
    workerSocketFrameEventMap.removeWorkerWebSocket(workerName);
    //need to fire back that worker was deleted
  }
  
  override def preStart() {
    log.info("Starting adminWorkerActor (adminWorkerActor under masterRouterActor) instance hashcode # {}", this.hashCode());  
  }
  
  override def postStop() {
    log.info("Stopping adminWorkerActor (adminWorkerActor under masterRouterActor) instance hashcode # {}",this.hashCode());
  }

}