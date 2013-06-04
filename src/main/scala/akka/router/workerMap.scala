package akka.router

import org.mashupbots.socko.events.WebSocketFrameEvent
import java.util.HashMap
import java.util.Map

object workerMap {
	val workSocketMap = new wMap;
}



class wMap {
  var workerSocketMap: Map[String, WebSocketFrameEvent] = new HashMap[String, WebSocketFrameEvent];
  
  def getWorkerWebSocket(workerName : String): WebSocketFrameEvent = {
    return workerSocketMap.get(workerName);  
  }
  
  def putWorkerWebSocket(workerName : String, workerWebSocket : WebSocketFrameEvent): Unit ={
    workerSocketMap.put(workerName, workerWebSocket);
  }
  
  def removeWorkerWebSocket(workerName : String): Unit ={
    workerSocketMap.remove(workerName);
  }
}