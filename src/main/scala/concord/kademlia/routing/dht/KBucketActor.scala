package concord.kademlia.routing.dht

import akka.actor.Actor
import concord.ConcordConfig
import concord.kademlia.routing.ActorNode
import concord.kademlia.routing.RoutingMessages.{FindClosest, FindClosestReply}
import concord.util.Logging


class KBucketActor(selfNode: ActorNode)(implicit config: ConcordConfig) extends Actor with Logging {
    self: KBucketSet.Provider =>

    import KBucketActor._

    private val kBuckets = newKBucketSet[ActorNode](selfNode, config.bucketsCapacity)

    override def receive: Receive = {
        case FindClosest(searchNode) =>
            sender ! FindClosestReply(selfNode, kBuckets.findClosestK(searchNode.nodeId))
        case Add(node) =>
            kBuckets.add(node)
    }

}

object KBucketActor {

    case class Add(node: ActorNode)

    trait Provider {
        def newKBucketActor(selfNode: ActorNode)(implicit config: ConcordConfig) = new KBucketActor(selfNode) with KBucketSet.Provider
    }

}
