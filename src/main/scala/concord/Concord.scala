package concord

import akka.actor.{Actor, ActorSystem}
import concord.identity.NodeId
import concord.kademlia.KademliaActor.Init
import concord.kademlia.{JoiningKadActor, KademliaActor}
import concord.util.Logging


class Concord(config: ConcordConfig) extends Logging {
    self: JoiningKadActor.Provider with WatchActor.Provider =>

    private val actorSystem = ActorSystem(config.systemName)

    val nodeId = config.nodeId match {
        case s if s.isEmpty => NodeId(config.host)
        case s: String => NodeId(s)
    }

    private val kadNode = config.existingNode match {
        case Some(node) => actorSystem.actorOf(newJoiningKademliaActor[Int](nodeId, node)(config), KademliaActor.nodeName)
        case _ => actorSystem.actorOf(newKademliaActor[Int](nodeId)(config), KademliaActor.nodeName)
    }

    actorSystem.actorOf(newWatchActor(kadNode), "watcherActor")

    log.info(s"\n\nStarting Concord with following info:\n\n$config\n\n$nodeId\n\n")

}


object Concord {

    def apply(config: ConcordConfig = ConcordConfig.fromFile): Concord =
        new Concord(config) with JoiningKadActor.Provider with WatchActor.Provider

}
