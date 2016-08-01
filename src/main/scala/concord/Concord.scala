package concord

import akka.actor.{ActorSystem, Props}
import concord.identity.NodeId
import concord.kademlia.KademliaActor.Init
import concord.kademlia.routing.RemoteNode
import concord.kademlia.{JoiningKadActor, KademliaActor}
import org.slf4j.LoggerFactory


class Concord(config: ConcordConfig) {
    self: JoiningKadActor.Provider =>

    val log = LoggerFactory.getLogger(getClass)

    private val actorSystem = ActorSystem()

    val nodeId = config.nodeId match {
        case s if s.isEmpty => NodeId(config.host)
        case s: String => NodeId(s)
    }

    val remoteNode = RemoteNode(config.host, nodeId)

    private val kadNode = config.existingNode match {
        case Some(node) => actorSystem.actorOf(Props(newJoiningKademliaActor[Int](nodeId, node)(config)), KademliaActor.nodeName)
        case _ => actorSystem.actorOf(Props(newKademliaActor[Int](nodeId)(config)), KademliaActor.nodeName)
    }

    kadNode ! Init

}


object Concord {

    def apply(config: ConcordConfig = ConcordConfig.fromFile): Concord = new Concord(config) with JoiningKadActor.Provider

}
