package messages

class workerJSONMessage (worker : String, data : String, wsEventObject: Object) {
  def workerName = worker;
  def dataOperation = data;
  def websocketEventObject = wsEventObject;

}