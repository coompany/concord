package concord.kademlia.routing.lookup

import akka.actor.{ActorRef, FSM}
import concord.identity.NodeId
import concord.kademlia.routing.ActorNode
import concord.kademlia.routing.RoutingMessages.{FindClosest, FindClosestReply}
import concord.kademlia.routing.lookup.LookupActor.{Data, State}


class LookupActor(selfNode: ActorNode, kBucketActor: ActorRef) extends FSM[State, Data] {

    import LookupActor._

    startWith(Initial, Empty)

    when(Initial) {
        case Event(request: FindClosest, _) =>
            kBucketActor forward request
            goto(WaitForLocalNodes) using Lookup(request.sender.nodeId, sender)
    }

    when(WaitForLocalNodes) {
        case Event(reply: FindClosestReply, request: Lookup) =>
            val nodes = selfNode :: reply.nodes
            stay
    }

}

object LookupActor {

    trait State
    case object Initial extends State
    case object WaitForLocalNodes extends State
    case object QueryNode extends State
    case object GatherNode extends State
    case object Finalize extends State

    trait Data
    case object Empty extends Data
    case class Lookup(nodeId: NodeId, sender: ActorRef) extends Data

    trait Provider {
        def newLookupActor(selfNode: ActorNode, kBucketActor: ActorRef) = new LookupActor(selfNode, kBucketActor)
    }

}
