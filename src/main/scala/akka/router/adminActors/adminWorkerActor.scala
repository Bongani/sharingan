package akka.router.adminActors

import akka.actor.Actor
import akka.actor.ActorLogging
import org.mashupbots.socko.events.WebSocketFrameEvent
import org.eligosource.eventsourced.core._
import java.util.HashMap
import java.util.Map
import messages.requestWorkerSocket

class adminWorkerActor (extension : EventsourcingExtension, processorID : Int) extends Actor with ActorLogging{
  
//  def workerSocketFrameEventMap = workerMap.workSocketMap;
  var workerSocketFrameEventMap: Map[String, WebSocketFrameEvent] = new HashMap[String, WebSocketFrameEvent]
  
  def receive = {
    
    case evtSourcedMessage: Message => {
      val message : adminMessage = evtSourcedMessage.event.asInstanceOf[adminMessage];
      val operation = message.operation;
      operation match {
        case "add" => {
          addWorker(message.worker, message.workerWebSocket);
          log.info("Worker added: " + message.worker);
        }
        case "delete" => {
          deleteWorker(message.worker);
        }
        case _=> log.info("Recieved unknown direct operation message for adminWorkerActor");
        log.info("Worker deleted: " + message.worker);
      }
    }
    
    case workerSocketRequest: requestWorkerSocket => {
      val workerWebSocket : WebSocketFrameEvent = workerSocketFrameEventMap.get(workerSocketRequest.name);
      sender ! workerWebSocket;
    }
    
    case sr @ SnapshotRequest(pid, snr, _) => {
        sr.process(workerSocketFrameEventMap)
        println(s"processed snapshot request for (snr = ${snr})")

      }
    case so @ SnapshotOffer(Snapshot(_, snr, time, wMap:Map[String, WebSocketFrameEvent])) => {
        workerSocketFrameEventMap = wMap
        println(s"accepted snapshot offer for (snr = ${snr} time = ${time}})")
      }
    
    
    case _=> log.info("unknown message")
  }
  
    
  def addWorker(workerName: String, webSocket: WebSocketFrameEvent): Unit ={
    workerSocketFrameEventMap.put(workerName, webSocket);
    //need to fire back that worker was added
  }
  
  def deleteWorker(workerName: String): Unit ={
    workerSocketFrameEventMap.remove(workerName);
    //need to fire back that worker was deleted
  }
  
  override def preStart() {
    log.info("Starting adminWorkerActor (adminWorkerActor under masterRouterActor) instance hashcode # {}", this.hashCode());  
  }
  
  override def postStop() {
    log.info("Stopping adminWorkerActor (adminWorkerActor under masterRouterActor) instance hashcode # {}",this.hashCode());
  }
  
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
   //need to overide with empty method so eventsourced recovery works
    }
  
  override def postRestart(reason: Throwable): Unit = {
    println("\n recovering messages for actor crashing\n")
    extension.recover(Seq(ReplayParams(processorID, snapshot = true)))
  }


}