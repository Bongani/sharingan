package test

import java.io.File
import java.io.FileOutputStream
import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.handlers.StaticContentHandler
import org.mashupbots.socko.handlers.StaticContentHandlerConfig
import org.mashupbots.socko.handlers.StaticFileRequest
import org.mashupbots.socko.infrastructure.CharsetUtil
import org.mashupbots.socko.infrastructure.IOUtil
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.routes._
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props
import akka.routing.FromConfig
import org.mashupbots.socko.handlers.StaticContentHandlerConfig
import akka.routing.RoundRobinRouter
import akka.routing.DefaultResizer

class benchmarkDispatcher {
  
  //val contentDir = createTempDir("content_")
  //val tempDir = createTempDir("temp_")
  //val staticContentHandlerConfig = StaticContentHandlerConfig(rootFilePaths = Seq(contentDir.getAbsolutePath),tempDir = tempDir)
  
  var actorLower : Int = 2;
  var actorUpper : Int = 15;
  val resizer = new DefaultResizer(lowerBound = actorLower, upperBound = actorUpper);
  val actorSystem = ActorSystem("BenchmarkActorSystem")
 // val staticContentHandlerRouter = actorSystem.actorOf(Props(new StaticContentHandler(staticContentHandlerConfig))
 //   .withRouter(FromConfig()).withDispatcher("my-dispatcher"), "static-file-router")
  val dynamicContentHandlerRouter = actorSystem.actorOf(Props[DynamicBenchmarkHandler].withRouter(RoundRobinRouter(resizer = Some(resizer))), "dynamic-file-router")
  
    val routes = Routes({
    case HttpRequest(request) => request match {
      /*case GET(Path("/small.html")) => {
        staticContentHandlerRouter ! new StaticFileRequest(request, new File(contentDir, "small.html"))
      }
      case GET(Path("/medium.txt")) => {
        staticContentHandlerRouter ! new StaticFileRequest(request, new File(contentDir, "medium.txt"))
      }
      case GET(Path("/big.txt")) => {
        staticContentHandlerRouter ! new StaticFileRequest(request, new File(contentDir, "big.txt"))
      }*/
      case GET(Path("/dynamic")) => {
        dynamicContentHandlerRouter ! request
        //actorSystem.actorOf(Props[DynamicBenchmarkHandler].withDispatcher("my-dispatcher")) ! request
      }
      case GET(Path("/favicon.ico")) => {
        request.response.write(HttpResponseStatus.NOT_FOUND)
      }
    }
  })
  
    def start() {
    // Create content
    //createContent(contentDir)

    // Start web server
    val webServer = new WebServer(WebServerConfig(), routes, actorSystem)
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run {
        webServer.stop()
        //contentDir.delete()
        //tempDir.delete()
      }
    })
    webServer.start()

    //System.out.println("Content directory is " + contentDir.getAbsolutePath)
    //System.out.println("87 bytes File   : http://localhost:8888/small.html")
    //System.out.println("200K File       : http://localhost:8888/medium.txt")
    //System.out.println("1MB File        : http://localhost:8888/big.txt")
    System.out.println("Dynamic Content : http://localhost:8888/dynamic")
  }
  
  
  /**
   * Returns a newly created temp directory
   *
   * @param namePrefix Prefix to use on the directory name
   * @return Newly created directory
   */
  /*private def createTempDir(namePrefix: String): File = {
    val d = File.createTempFile(namePrefix, "")
    d.delete()
    d.mkdir()
    d
  }*/

  /**
   * Create files for downloading
   */
  /*private def createContent(dir: File) {
    val buf = new StringBuilder()

    // medium.txt - 100K file
    buf.setLength(0)
    for (i <- 0 until (1024 * 100)) {
      buf.append('a')
    }

    val mediumFile = new File(dir, "medium.txt")
    val out = new FileOutputStream(mediumFile)
    out.write(buf.toString.getBytes(CharsetUtil.UTF_8))
    out.close()

    // big.txt - 1MB file
    buf.setLength(0)
    for (i <- 0 until (1024 * 1024)) {
      buf.append('a')
    }

    val bigFile = new File(dir, "big.txt")
    val out2 = new FileOutputStream(bigFile)
    out2.write(buf.toString.getBytes(CharsetUtil.UTF_8))
    out2.close()

    // copy over small foo.html (same file used by vertx)
    val fooFile = new File(dir, "small.html")
    val out3 = new FileOutputStream(fooFile)
    out3.write(IOUtil.readResource("foo.html"))
    out3.close()
  }*/

}