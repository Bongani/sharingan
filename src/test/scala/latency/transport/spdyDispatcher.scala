/*package latency.transport

import akka.actor.ActorLogging
import akka.actor.Actor
import java.io.File
import org.mashupbots.socko.webserver.SslConfig
import org.mashupbots.socko.webserver.HttpConfig
import org.mashupbots.socko.webserver.WebServerConfig
import org.mashupbots.socko.webserver.WebLogConfig
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.routes.Routes
import org.mashupbots.socko.routes.GET
import akka.actor.ActorRef
import akka.actor.ActorSystem

class spdyDispatcher(actorSystem: ActorSystem, HelloHandlerActor : ActorRef) extends Actor with ActorLogging {
  
  val routes = Routes({
    case GET(request) => {
      HelloHandlerActor ! request
    }
  })
  
  
  override def preStart(){
    //SPDY setup
  var workingDirectory = new java.io.File(".").getCanonicalPath();
  var keyStorePath = workingDirectory + "/keystore/server.jks"
  
  val keyStoreFile = new File(keyStorePath);  
  val keyStoreFilePassword = "password";
  val sslConfig = SslConfig(keyStoreFile, keyStoreFilePassword, None, None);
  val httpConfig = HttpConfig(spdyEnabled = true);
  val webServerConfig = WebServerConfig(hostname="0.0.0.0", webLog = Some(WebLogConfig()), ssl = Some(sslConfig), http = httpConfig)
  
  
  val webServer = new WebServer(webServerConfig, routes, actorSystem);
  
  //val webServer = new WebServer(WebServerConfig(), routes, actorSystem);
    
     
  webServer.start();
  println("Server started");
      
  
  }

}*/