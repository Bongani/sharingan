package akka.node

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.Props
import akka.voldemort.voldactors.voldCoordinator
import akka.messaging.masterMessagingActor
import akka.router.masterRouterActor
import configuration.storage
import akka.actor.ActorSystem

class systemNode (nodeName : String, actorSystem : ActorSystem) {

  //val system = ActorSystem.create("VoldertTestSystem", ConfigFactory.load().getConfig("AkkaConfig")); //ActorSystem("VoldertTestSystem");
  
  var voldActorName = nodeName + "_voldemorCordActor";
  var voldActor = actorSystem.actorOf(Props[voldCoordinator], name = voldActorName);


  //need to create configurations for these actors
  var messagingMasterName = nodeName + "_messagingMasterActor";  
  var messagingMasterActor = actorSystem.actorOf(Props[masterMessagingActor], name = messagingMasterName);
  var routerMasterName = nodeName + "_masterRouterWorkerActor";  
  var routingWorkerMasterActor = actorSystem.actorOf(Props[masterRouterActor], name = routerMasterName);
  
}