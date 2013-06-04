package messages

import org.mashupbots.socko.events.WebSocketFrameEvent

class workerMessage(worker : String, data : String, wsEvent: WebSocketFrameEvent) {
  def workerName = worker;
  def dataOperation = data;
  def websocketEvent = wsEvent;
}