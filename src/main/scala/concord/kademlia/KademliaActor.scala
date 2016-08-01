package concord.kademlia

import akka.actor.{ActorRef, FSM, Props}
import akka.util.Timeout
import concord.ConcordConfig
import concord.identity.NodeId
import concord.kademlia.KademliaActor._
import concord.kademlia.routing.RoutingMessages.{FindNode, FindNodeReply, PingRequest, PongReply}
import concord.kademlia.routing.{ActorNode, RoutingActor}
import concord.kademlia.store.{InMemoryStore, StoreActor}
import concord.util.Host

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
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

    implicit val timeout: Timeout = FiniteDuration(30, SECONDS)

}

class KademliaActor[V](nodeId: NodeId)(implicit config: ConcordConfig) extends FSM[State, Data] {
    this: RoutingActor.Provider =>

    protected val selfNode = ActorNode(self, nodeId)

    protected val routingActor = context.system.actorOf(Props(newRoutingActor(selfNode)))
    protected val storeActor = context.system.actorOf(Props(new StoreActor[V] with InMemoryStore[V]))

    startWith(Running, Empty)

    when(Running) {
        case request: Event =>
            routingActor forward request
            stay
    }

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
    this: RoutingActor.Provider =>

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
            actorRef ! PingRequest(selfNode)
            goto(Joining)
    }

    when(Joining) {
        case Event(PongReply, Empty) =>
            routingActor ! FindNode(selfNode, selfNode.nodeId)
            stay
        case Event(reply: FindNodeReply, Empty) =>
            goto(Running)
    }

}
