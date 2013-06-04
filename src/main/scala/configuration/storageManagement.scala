package configuration

import voldemort.client.DefaultStoreClient
import java.util.Map
import java.util.HashMap
import java.io.File
import scala.xml.XML
import java.util.HashSet
import java.util.Set
import scala.collection.JavaConversions._
import voldemort.client.ClientConfig
import voldemort.client.protocol.RequestFormatType
import voldemort.client.SocketStoreClientFactory
import voldemort.client.StoreClientFactory
import scala.tools.ant.sabbus.Break
import scala.util.control.Breaks

case class serverInfo(serverID: String, host: String , port: String);

class storageManagement {
  
  val storageMap: Map[String, DefaultStoreClient[Object, Object]] = new HashMap[String, DefaultStoreClient[Object, Object]];
  //key: storagename, value: storageclient 
  var serverSet: Set[serverInfo] = new HashSet[serverInfo];
  var storeNameSet: Set[String] = new HashSet[String];
  
  
  def getStorage(storageName : String): DefaultStoreClient[Object, Object] = {
    return storageMap.get(storageName);    
  }
  
  def putStorage(storageName : String, storage : DefaultStoreClient[Object, Object]): Unit ={
    storageMap.put(storageName, storage);
  }
  
  def removeStorage(storageName : String): Unit ={
    storageMap.remove(storageName);
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
  
  def readClusterContent(clusterXML : scala.xml.Elem) : Unit ={
    var serverId: String = null;
    var host: String = null;
    var port: String = null;
    
    if (clusterXML == null){
      print("no contents in the cluster.xml file");
      exit(1);
    }
    
    for (server <- clusterXML \ "server"){
      serverId = (server \ "id").text;
      host = (server \ "host").text;
      port = (server \ "socket-port").text;
      var servInfo = new serverInfo(serverId, host, port);
      serverSet.add(servInfo);
    }
    
  }
  
  def readStoreContent(storeXML : scala.xml.Elem) : Unit = {
    var storeName: String = null;
    
    if (storeXML == null){
      print("no contents in the store.xml file");
      exit(1);
    }
    
    for (store <- storeXML \ "store"){
      storeName = (store \ "name").text;
      storeNameSet.add(storeName);
    }
    
  }
  
  //only used to connect stores at the beginning
  def connectAllStores() : Unit = {
     
    var storeName: String = null;
    var hostNumbers: Integer = serverSet.size();
    
    storeNameSet.foreach(storeName => {
      var count: Integer = 0;
      var succesfulSetup: Boolean = false;
      
      val loop = new Breaks;
      loop.breakable {
        serverSet.foreach(servInfo => {
        if (!succesfulSetup){
          var bootstrapURL: String = "tcp://" + servInfo.host + ":" + servInfo.port;
          var clientConfig: ClientConfig = new ClientConfig().setBootstrapUrls(bootstrapURL).setEnableLazy(false).setRequestFormatType(RequestFormatType.VOLDEMORT_V3);
          var clientStore: DefaultStoreClient[Object, Object] = null;
          
          try {
            var factory: StoreClientFactory = new SocketStoreClientFactory(clientConfig);
          	clientStore = factory.getStoreClient(storeName).asInstanceOf[DefaultStoreClient[Object, Object]];
          	// adminClient = new AdminClient(bootstrapUrl, new AdminClientConfig());
          	
          	storageMap.put(storeName, clientStore);
          	succesfulSetup = true;
          	println("Connected to voldemort server " + storeName);
          	loop.break;
          	
          } catch{
            case e: Exception => println("Could not connect to server: " + e.getMessage());
          }
          
        }
      })
        
      }
      
    })
  }
  
  def startupSetup(userGivenPath: String): Unit = {
    var workingDirectory = new java.io.File(".").getCanonicalPath();
    var folderPath: String = workingDirectory + userGivenPath;
    var configFolder = new File(folderPath);
    
    if (configFolder.exists()){
      //cluster information
      var clusterXML : scala.xml.Elem = readXMLFile(folderPath, "cluster.xml");
      readClusterContent(clusterXML);
      //store information
      var storeXML : scala.xml.Elem = readXMLFile(folderPath, "stores.xml");
      readStoreContent(storeXML);
      connectAllStores();
      
    } else {
      println(" \n \n" + "Error: The folder for storage configurations could not be found. Path given: " + configFolder + " \n \n");
      exit(1);
    }
  }

}