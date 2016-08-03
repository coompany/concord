package concord.kademlia.routing.dht

import akka.actor.{Actor, ActorLogging, Props}
import concord.ConcordConfig
import concord.kademlia.routing.{ActorNode, RemoteNode}


class KBucketActor(selfNode: RemoteNode)(implicit config: ConcordConfig) extends Actor with ActorLogging {
    self: KBucketSet.Provider =>

    import KBucketMessages._

    log.info(s"Creating kbuckets of capacity ${config.bucketsCapacity}")
    private val kBuckets = newKBucketSet[ActorNode](selfNode, config.bucketsCapacity)

    override def receive: Receive = {
        case FindKClosest(searchNode) =>
            sender ! FindKClosestReply(selfNode, searchNode, kBuckets.findClosestK(searchNode))
        case Add(node: ActorNode) =>
            kBuckets.add(node)
    }

}

object KBucketActor {

    trait Provider {
        def newKBucketActor(selfNode: RemoteNode)(implicit config: ConcordConfig) =
            Props(new KBucketActor(selfNode) with KBucketSet.Provider)
    }

}
