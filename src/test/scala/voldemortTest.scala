import akka.actor.ActorSystem
import akka.actor.Props
import akka.voldemort.voldactors.voldCoordinator

object voldemortTest {

  def main(args: Array[String]): Unit = {
    
    val system = ActorSystem("VoldertTestSystem");
    val myActor = system.actorOf(Props[voldCoordinator], name = "testActor");
    
    
  }

}