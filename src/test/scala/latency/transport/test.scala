package latency.transport

import akka.actor.ActorSystem
import akka.actor.Props
import latency.transport.HelloHandler
import latency.transport.dispatcher

object test {

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem("LatencyActorSystem");
    val helloHandler = actorSystem.actorOf(Props[HelloHandler], name = "HelloHandlerActor");
    
    val webServerActor = actorSystem.actorOf(Props(new dispatcher(actorSystem, helloHandler)), name = "dispatcherActor");
  }

}