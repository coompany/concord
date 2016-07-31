package concord.kademlia.routing

import akka.actor.{Actor, Props}
import concord.ConcordConfig
import concord.identity.NodeId
import concord.kademlia.routing.lookup.LookupActor
import concord.kademlia.routing.dht.KBucketActor
import concord.kademlia.routing.dht.KBucketActor.Add
import concord.util.{Host, Logging}


class RoutingActor(nodeId: NodeId)(implicit val config: ConcordConfig) extends Actor with Logging {
    self: KBucketActor.Provider with LookupActor.Provider =>

    import RoutingMessages._

    private val kBucketActor = context.system.actorOf(Props(newKBucketActor(nodeId)))

    override def receive = {
        case PingRequest(senderId) =>
            kBucketActor ! Add(RemoteNode(Host(sender().path.address.hostPort), senderId))
            sender ! PongReply(nodeId)
        case request @ FindClosest(findNodeId) =>
            context.system.actorOf(Props(newLookupActor(kBucketActor))) forward request
    }

}

object RoutingActor {

    trait Provider {
        def newRoutingActor(nodeId: NodeId)(implicit config: ConcordConfig) =
            new RoutingActor(nodeId) with KBucketActor.Provider with LookupActor.Provider
    }

}
