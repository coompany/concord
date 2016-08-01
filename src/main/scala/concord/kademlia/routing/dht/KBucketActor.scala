package concord.kademlia.routing.dht

import akka.actor.Actor
import concord.ConcordConfig
import concord.kademlia.routing.ActorNode
import concord.util.LoggingActor


class KBucketActor(selfNode: ActorNode)(implicit config: ConcordConfig) extends Actor with LoggingActor {
    self: KBucketSet.Provider =>

    import KBucketMessages._

    log.info(s"Creating kbuckets of capacity ${config.bucketsCapacity}")
    private val kBuckets = newKBucketSet[ActorNode](selfNode, config.bucketsCapacity)

    override def receive: Receive = {
        case FindKClosest(searchNode) =>
            sender ! FindKClosestReply(selfNode.nodeId, kBuckets.findClosestK(searchNode))
        case Add(node: ActorNode) =>
            kBuckets.add(node)
    }

}

object KBucketActor {

    trait Provider {
        def newKBucketActor(selfNode: ActorNode)(implicit config: ConcordConfig) = new KBucketActor(selfNode) with KBucketSet.Provider
    }

}
