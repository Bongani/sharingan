package akka.node

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorSystem
import java.util.HashMap
import java.util.Map
import akka.actor.PoisonPill
import java.io.File
import scala.xml.XML
import org.eligosource.eventsourced.core._
import akka.webserver.nodeRequest



class nodeManager (actorSystem : ActorSystem, extension : EventsourcingExtension) extends Actor with ActorLogging{
  
  var nodeMap: Map[String, systemNode] = new HashMap[String, systemNode];
  
  def receive ={
    /*case message: nodeManagerMessage => {
      val operation = message.operation;
      
      operation match {
        case "create" => createNewNode(message.nodeID);
        case "delete" => deleteNode(message.nodeID);
        case _ => log.info("Recieved unknown operation message for the  node manager: nodeManager");
      }     
      
    }*/
    case "configNodes" => {
      println("Configuring nodes");
      nodeConfig;
    }
    
    case evtSourcedMessage: Message => {
      //topicMessage
      val message: nodeManagerMessage = evtSourcedMessage.event.asInstanceOf[nodeManagerMessage];
     
      val operation = message.operation;
      
      operation match {
        case "create" => createNewNode(message.nodeName, message.nodeID);
        case "delete" => deleteNode(message.nodeName);
        case _ => log.info("Recieved unknown operation message for the  node manager: nodeManager");
      } 
    }
    
    case nodeRequestMessage: nodeRequest => {
      val node : systemNode = nodeMap.get(nodeRequestMessage.nodeName);
      sender ! node;
    }
    
    case sr @ SnapshotRequest(pid, snr, _) => {
        sr.process(nodeMap)
        println(s"processed snapshot request for (snr = ${snr})")

      }
    case so @ SnapshotOffer(Snapshot(_, snr, time, nMap: Map[String,systemNode])) => {
        nodeMap = nMap.asInstanceOf[Map[String, akka.node.systemNode]];
        println(s"accepted snapshot offer for (snr = ${snr} time = ${time}})")
      }
    
    case _=> log.info("unknown message");
  }
  
  def createNewNode(nodeName : String, nodeID : Int): Unit ={
    val node = new systemNode(nodeName, nodeID,actorSystem, extension);
    nodeMap.put(nodeName, node);
    
  }
  
  def deleteNode(nodeName : String): Unit ={
    //actor ! PoisonPill
    val node = nodeMap.get(nodeName);
    nodeMap.remove(nodeName);
    node.voldActor ! PoisonPill;
    node.messagingMasterActor ! PoisonPill;
    node.routingWorkerMasterActor ! PoisonPill;
  }
  
  def nodeConfig() : Unit = {
    var workingDirectory = new java.io.File(".").getCanonicalPath();
    var folderPath: String = workingDirectory + "/config/Nodes/";
    var configFolder = new File(folderPath);
    
    if (configFolder.exists()){
      //store actor information
      var nodeXML : scala.xml.Elem = readXMLFile(folderPath, "nodecluster.xml");
      //readStoreContent(storageXML);
      
      if (nodeXML != null){              
        print("no contents in the nodecluster.xml file");        
      } else {
        for (server <- nodeXML \ "node"){
        var id = (server \ "id").text;
        var nodeID = id.toInt;
        var nodeName = (server \ "name").text;
        var configurationID : Int = 10 * nodeID;
        
        createNewNode(nodeName, configurationID);
        }
        
      }
      
      
      
      
    } else {
      println(" \n \n" + "Error: The folder for creating nodes could not be found. Path given: " + configFolder + " \n \n");
      println("You may create nodes through admin")
     
    }
  }
  
  
  def readXMLFile(path: String, fileName: String): scala.xml.Elem ={
    
    var xmlFile = new File(path, fileName); 
    if (xmlFile.exists()){
      var xmlContent = XML.loadFile(xmlFile);
      println("Read XML file for: " + fileName);
      return xmlContent;
    } else {
      println("File not found for XML reading: " + fileName);
      return null;
    }
    
  }

}