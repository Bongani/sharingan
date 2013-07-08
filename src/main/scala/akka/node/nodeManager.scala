package akka.node

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorSystem
import java.util.HashMap
import java.util.Map
import akka.actor.PoisonPill
import java.io.File
import scala.xml.XML

class nodeManager (actorSystem : ActorSystem) extends Actor with ActorLogging{
  
  var nodeMap: Map[String, systemNode] = new HashMap[String, systemNode];
  
  def receive ={
    case message: nodeManagerMessage => {
      val operation = message.operation;
      
      operation match {
        case "create" => createNewNode(message.nodeID);
        case "delete" => deleteNode(message.nodeID);
        case _ => log.info("Recieved unknown operation message for the  node manager: nodeManager");
      }     
      
    }
    
    case _=> log.info("unknown message");
  }
  
  def createNewNode(nodeName : String): Unit ={
    val node = new systemNode(nodeName, actorSystem);
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
        var nodeID = (server \ "id").text;
        createNewNode(nodeID);
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