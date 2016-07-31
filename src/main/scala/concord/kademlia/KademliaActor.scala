package concord.kademlia

import akka.actor.{ActorRef, FSM, Props}
import concord.ConcordConfig
import concord.identity.NodeId
import concord.kademlia.KademliaActor._
import concord.kademlia.routing.RoutingActor
import concord.kademlia.routing.RoutingMessages.{FindClosest, PingRequest, PongReply}
import concord.kademlia.store.{InMemoryStore, StoreActor}
import concord.util.Host

import scala.util.{Failure, Success}


object KademliaActor {

    case object Init

    trait State
    case object Running extends State

    trait Data
    case object Empty extends Data

    trait Provider {
        def newKademliaActor[V](nodeId: NodeId)(implicit config: ConcordConfig): KademliaActor[V] =
            new KademliaActor[V](nodeId) with RoutingActor.Provider
    }

    val nodeName = "concordKademlia"

}

class KademliaActor[V](nodeId: NodeId)(implicit config: ConcordConfig) extends FSM[State, Data] {
    self: RoutingActor.Provider =>

    startWith(Running, Empty)

    protected val routingActor = context.system.actorOf(Props(newRoutingActor(nodeId)))
    protected val storeActor = context.system.actorOf(Props(new StoreActor[V] with InMemoryStore[V]))

}


object JoiningKadActor {

    case object PingExisting extends State
    case object Joining extends State

    trait Provider extends KademliaActor.Provider {
        def newJoiningKademliaActor[V](nodeId: NodeId, existingNode: Host)(implicit config: ConcordConfig) =
            new JoiningKadActor[V](nodeId, existingNode) with RoutingActor.Provider
    }

}

class JoiningKadActor[V](nodeId: NodeId, existingNode: Host)(implicit config: ConcordConfig) extends KademliaActor[V](nodeId) {
    self: RoutingActor.Provider =>

    import JoiningKadActor._

    startWith(PingExisting, Empty)

    when(PingExisting) {
        case Event(Init, Empty) =>
            context.system.actorSelection(existingNode.toAkka(context.system.name, nodeName)).resolveOne().onComplete {
                case Success(ref) => context.self ! ref
                case Failure(error) => throw new RuntimeException(error)
            }
            stay
        case Event(actorRef: ActorRef, Empty) =>
            actorRef ! PingRequest
            goto(Joining)
    }

    when(Joining) {
        case Event(PongReply, Empty) =>
            routingActor ! FindClosest(nodeId)
            stay
    }

}
