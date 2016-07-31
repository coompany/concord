package concord.kademlia.routing.dht

import akka.actor.Actor
import concord.ConcordConfig
import concord.identity.NodeId
import concord.kademlia.routing.RemoteNode
import concord.kademlia.routing.RoutingMessages.{FindClosest, FindClosestReply}
import concord.util.Logging


class KBucketActor(nodeId: NodeId)(implicit config: ConcordConfig) extends Actor with Logging {
    self: KBucketSet.Provider =>

    import KBucketActor._

    private val kBuckets = newKBucketSet[RemoteNode](nodeId, config.bucketsCapacity)

    override def receive: Receive = {
        case FindClosest(searchNodeId) =>
            sender ! FindClosestReply(nodeId, kBuckets.findClosestK(searchNodeId))
        case Add(remoteNode) =>
            kBuckets.add(remoteNode)
    }

}

object KBucketActor {

    case class Add(remoteNode: RemoteNode)

    trait Provider {
        def newKBucketActor(nodeId: NodeId)(implicit config: ConcordConfig) = new KBucketActor(nodeId) with KBucketSet.Provider
    }

}
