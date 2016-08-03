package concord.kademlia

import akka.actor.{Actor, ActorRef, FSM, Props}
import akka.util.Timeout
import concord.ConcordConfig
import concord.identity.NodeId
import concord.kademlia.KademliaActor._
import concord.kademlia.routing.RoutingMessages._
import concord.kademlia.routing.{ActorNode, RoutingActor}
import concord.util.Host

import scala.concurrent.duration._
import scala.util.{Failure, Success}


object KademliaActor {

    case object Init

    trait State
    case object Running extends State

    trait Data
    case object Empty extends Data

    trait Provider {
        def newKademliaActor[V](nodeId: NodeId)(implicit config: ConcordConfig) =
            Props(new KademliaActor[V](nodeId) with RoutingActor.Provider)
    }

    val nodeName = "concordKademlia"

    implicit val timeout: Timeout = 5 seconds

}

class KademliaActor[V](nodeId: NodeId)(implicit config: ConcordConfig) extends FSM[State, Data] {
    this: RoutingActor.Provider =>

    protected val selfNode = ActorNode(self, nodeId)

    protected val routingActor = context.actorOf(newRoutingActor(selfNode), "routingActor")

    startWith(Running, Empty)

    when(Running) (init orElse remoteReply andThen(x => stay()))

    private def init: PartialFunction[Event, Unit] = {
        case Event(Init, Empty) =>
            log.info("Starting new Kademlia net")
    }

    protected def remoteReply: PartialFunction[Event, Unit] = {
        case Event(request: NodeRequest, _) =>
            log.info("Got request, forwarding to routing actor")
            routingActor forward request
    }

    initialize()

}


object JoiningKadActor {

    case object PingExisting extends State
    case object Joining extends State

    trait Provider extends KademliaActor.Provider {
        def newJoiningKademliaActor[V](nodeId: NodeId, existingNode: Host)(implicit config: ConcordConfig) =
            Props(new JoiningKadActor[V](nodeId, existingNode) with RoutingActor.Provider)
    }

}

class JoiningKadActor[V](nodeId: NodeId, existingNode: Host)(implicit config: ConcordConfig) extends KademliaActor[V](nodeId) {
    this: RoutingActor.Provider =>

    import JoiningKadActor._

    startWith(PingExisting, Empty)

    when(PingExisting) {
        case Event(Init, Empty) =>
            val actorPath = existingNode.toAkka(context.system.name, nodeName)
            log.info(s"Connecting to existing Kademlia net via $actorPath")
            import context.dispatcher
            context.actorSelection(actorPath).resolveOne().onComplete {
                case Success(ref) =>
                    log.info(s"Bootstrapping actor found: $ref")
                    context.self ! ref
                case Failure(error) => throw new RuntimeException(error)
            }
            stay
        case Event(actorRef: ActorRef, Empty) =>
            log.info("Sending ping request")
            self ! NodeRequest(actorRef, PingRequest(selfNode))
            log.info("Ping request sent, entering joining state")
            goto(Joining)
    }

    when(Joining) (remoteReply andThen(x => stay) orElse {
        case Event(pong: PongReply, Empty) =>
            log.info("Got pong reply, sending find node request")
            routingActor ! AddToBuckets(pong.sender)
            routingActor ! NodeRequest(self, FindNode(selfNode, selfNode.nodeId, local = false))
            stay
        case Event(reply: FindNodeReply, Empty) =>
            log.info("Got find node reply, going into running")
            goto(Running)
    })

}
