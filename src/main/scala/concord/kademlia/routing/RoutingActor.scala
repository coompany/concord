package concord.kademlia.routing

import akka.actor.{Actor, Props}
import concord.ConcordConfig
import concord.kademlia.routing.dht.{KBucketActor, KBucketMessages}
import concord.kademlia.routing.lookup.LookupActor
import concord.util.LoggingActor


class RoutingActor(selfNode: ActorNode)(implicit val config: ConcordConfig) extends Actor with LoggingActor {
    self: KBucketActor.Provider with LookupActor.Provider =>

    import KBucketMessages._
    import RoutingMessages._

    private val kBucketActor = context.system.actorOf(Props(newKBucketActor(selfNode)))

    override def receive = {
        case PingRequest(senderNode) =>
            log.info("Got ping request, sending pong reply")
            kBucketActor ! Add(ActorNode(sender, senderNode.nodeId))
            sender ! PongReply(selfNode)
        case request: FindNode =>
            context.system.actorOf(Props(newLookupActor(selfNode, kBucketActor, config.bucketsCapacity, config.alpha))) forward request
    }

}

object RoutingActor {

    trait Provider {
        def newRoutingActor(selfNode: ActorNode)(implicit config: ConcordConfig) =
            new RoutingActor(selfNode) with KBucketActor.Provider with LookupActor.Provider
    }

}
