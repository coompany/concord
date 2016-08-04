package concord.kademlia.routing.dht

import akka.actor.{Actor, ActorLogging, Props}
import concord.ConcordConfig
import concord.kademlia.routing.RoutingMessages.Node


class KBucketActor(selfNode: Node)(implicit config: ConcordConfig) extends Actor with ActorLogging {
    self: KBucketSet.Provider =>

    import KBucketMessages._

    log.info(s"Creating kbuckets of capacity ${config.bucketsCapacity}")
    private val kBuckets = newKBucketSet[Node](selfNode, config.bucketsCapacity)

    override def receive: Receive = {
        case FindKClosest(searchNode) =>
            sender ! FindKClosestReply(selfNode, searchNode, kBuckets.findClosestK(searchNode))
        case Add(node: Node) =>
            kBuckets.add(node)
    }

}

object KBucketActor {

    trait Provider {
        def newKBucketActor(selfNode: Node)(implicit config: ConcordConfig) =
            Props(new KBucketActor(selfNode) with KBucketSet.Provider)
    }

}
