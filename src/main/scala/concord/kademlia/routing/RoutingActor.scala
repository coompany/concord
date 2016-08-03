package concord.kademlia.routing

import akka.actor.{Actor, Props}
import concord.ConcordConfig
import concord.kademlia.routing.dht.KBucketActor
import concord.kademlia.routing.dht.KBucketMessages.{Add, FindKClosest}
import concord.kademlia.routing.lookup.LookupActor
import concord.util.LoggingActor


class RoutingActor(selfNode: ActorNode)(implicit val config: ConcordConfig) extends Actor with LoggingActor {
    self: KBucketActor.Provider with LookupActor.Provider =>

    import RoutingMessages._

    private val kBucketActor = context.actorOf(newKBucketActor(selfNode), "kBucketActor")

    override def receive = {
        case req @ NodeRequest(recipient, PingRequest(senderNode)) if recipient != selfNode.ref =>
            recipient forward req
        case req @ NodeRequest(_, PingRequest(senderNode)) =>
            log.info("Got ping request, sending pong reply")
            addToBuckets(req)
            sender ! PongReply(selfNode)
        case req @ NodeRequest(_, request: FindNode) if !request.local =>
            log.info(s"Got lookup find node request against ${request.searchId}")
            addToBuckets(req)
            val lookupActor = context.actorOf(newLookupActor(selfNode, kBucketActor, config.bucketsCapacity, config.alpha), "lookupActor")
            lookupActor forward request
        case req @ NodeRequest(_, FindNode(senderNode, searchId, _)) =>
            log.info(s"Got local find node request for $searchId")
            addToBuckets(req)
            kBucketActor forward FindKClosest(searchId)
            log.info(s"Sent local find node response for $searchId")
        case AddToBuckets(nodeToAdd) =>
            kBucketActor ! Add(nodeToAdd)

    }

    private def addToBuckets(nodeRequest: NodeRequest) =
        if (nodeRequest.request.sender.nodeId != selfNode.nodeId)
            kBucketActor forward Add(ActorNode(nodeRequest.request.sender.ref, nodeRequest.request.sender.nodeId))

}

object RoutingActor {

    trait Provider {
        def newRoutingActor(selfNode: ActorNode)(implicit config: ConcordConfig) =
            Props(new RoutingActor(selfNode) with KBucketActor.Provider with LookupActor.Provider)
    }

}
