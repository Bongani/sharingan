package messages

import org.mashupbots.socko.events.WebSocketFrameEvent

class topicMessage(topicName: String, task: String, wsEvent: WebSocketFrameEvent) {
  
  def name = topicName;
  def messageTask = task;
  def webSocketEvent = wsEvent;

}