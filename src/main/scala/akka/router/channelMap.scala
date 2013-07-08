package akka.router

import java.util.HashMap
import java.util.Map
import org.mashupbots.socko.events.WebSocketFrameEvent

object channelMap {
	val webSocketChannelSocketMap = new cMap;
}

class cMap {
  var channelClientMap: Map[Int, WebSocketFrameEvent] = new HashMap[Int, WebSocketFrameEvent];
  
  def getClientWebSocket(channelInt : Int): WebSocketFrameEvent = {
    return channelClientMap.get(channelInt);  
  }
  
  def putClientWebSocket(channelInt : Int, clientWebSocket : WebSocketFrameEvent): Unit ={
    channelClientMap.put(channelInt, clientWebSocket);
  }
  
  def removeClientWebSocket(channelInt : Int): Unit ={
    channelClientMap.remove(channelInt);
  }
}