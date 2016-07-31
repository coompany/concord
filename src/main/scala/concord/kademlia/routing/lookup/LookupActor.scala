package concord.kademlia.routing.lookup

import akka.actor.{ActorRef, FSM}
import concord.identity.NodeId
import concord.kademlia.routing.RoutingMessages.{FindClosest, FindClosestReply}
import concord.kademlia.routing.lookup.LookupActor.{Data, State}


class LookupActor(kBucketActor: ActorRef) extends FSM[State, Data] {

    import LookupActor._

    startWith(Initial, Empty)

    when(Initial) {
        case request @ Event(FindClosest(searchId), _) =>
            kBucketActor forward request
            goto(WaitForLocalNodes) using Lookup(searchId, sender)
    }

    when(WaitForLocalNodes) {
        case Event(reply: FindClosestReply, request: Lookup) =>
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
        def newLookupActor(kBucketActor: ActorRef) = new LookupActor(kBucketActor)
    }

}
