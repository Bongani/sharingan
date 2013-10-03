package messages

import org.mashupbots.socko.events.WebSocketFrameEvent

/*case class workerMessage(worker : String, data : String, wsEvent: WebSocketFrameEvent, response : Boolean) {
  def workerName = worker;
  def dataOperation = data;
  def websocketEvent = wsEvent;
  def expectingResponse = response;
}*/

case class workerMessage(workerName : String, dataOperation : String, websocketEvent: WebSocketFrameEvent, expectingResponse : Boolean)