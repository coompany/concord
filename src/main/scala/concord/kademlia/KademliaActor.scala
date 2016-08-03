package concord.kademlia

import akka.actor.{FSM, Props}
import akka.util.Timeout
import concord.ConcordConfig
import concord.identity.NodeId
import concord.kademlia.KademliaActor._
import concord.kademlia.routing.RoutingMessages._
import concord.kademlia.routing.{RemoteNode, RoutingActor}
import concord.kademlia.udp.ListenerActor.ListenerMessage
import concord.kademlia.udp.SenderActor.{SenderMessage, SenderReady}
import concord.kademlia.udp.{ListenerActor, SenderActor}
import concord.util.Host

import scala.concurrent.duration._


object KademliaActor {

    case object Init

    trait State
    case object WaitSender extends State
    case object Running extends State

    trait Data
    case object Empty extends Data

    trait Provider {
        def newKademliaActor[V](nodeId: NodeId)(implicit config: ConcordConfig) =
            Props(new KademliaActor[V](nodeId) with SenderActor.Provider with ListenerActor.Provider with RoutingActor.Provider)
    }

    val nodeName = "concordKademlia"

    implicit val timeout: Timeout = 5 seconds

}

class KademliaActor[V](nodeId: NodeId)(implicit config: ConcordConfig) extends FSM[State, Data] {
    this: SenderActor.Provider with ListenerActor.Provider with RoutingActor.Provider =>

    protected val selfNode = RemoteNode(config.host, nodeId)

    protected val listenerActor = context.actorOf(newListenerActor(config.host, context.self), "listenerActor")
    protected val senderActor = context.actorOf(newSenderActor(context.self), "senderActor")
    protected val routingActor = context.actorOf(newRoutingActor(selfNode, senderActor), "routingActor")

    protected def afterSenderState: State = goto(Running)

    protected def initMessage() = self ! Init

    startWith(WaitSender, Empty)

    when(WaitSender) (waitForSender)

    protected def waitForSender: StateFunction = {
        case Event(SenderReady, _) =>
            log.info("Sender actor is ready")
            initMessage()
            afterSenderState
    }

    when(Running) (init orElse remoteReply andThen(x => stay()))

    private def init: PartialFunction[Event, Unit] = {
        case Event(Init, Empty) =>
            log.info("Starting new Kademlia net")
    }

    protected def remoteReply: PartialFunction[Event, Unit] = {
        case Event(message: Message, _) =>
            log.info(s"Got request $message, forwarding to routing actor")
            routingActor forward message
        case Event(request: ListenerMessage, _) =>
            log.info(s"Got listener message, forwarding ${request.message}")
            self forward request.message
    }

}


object JoiningKadActor {

    case object PingExisting extends State
    case object Joining extends State

    trait Provider extends KademliaActor.Provider {
        def newJoiningKademliaActor[V](nodeId: NodeId, existingNode: Host)(implicit config: ConcordConfig) =
            Props(new JoiningKadActor[V](nodeId, existingNode) with SenderActor.Provider with ListenerActor.Provider with RoutingActor.Provider)
    }

}

class JoiningKadActor[V](nodeId: NodeId, existingNode: Host)(implicit config: ConcordConfig) extends KademliaActor[V](nodeId) {
    this: SenderActor.Provider with ListenerActor.Provider with RoutingActor.Provider =>

    import JoiningKadActor._

    override protected def afterSenderState: State = goto(PingExisting)

    when(PingExisting) {
        case Event(Init, Empty) =>
            log.info(s"Connecting to existing Kademlia net via $existingNode")
            self ! existingNode
            stay
        case Event(remoteHost: Host, Empty) =>
            log.info("Sending ping request")
            senderActor ! SenderMessage(remoteHost, PingRequest(selfNode))
            goto(Joining)
    }

    when(Joining) (joiningSF orElse remoteReply andThen(x => stay))

    private def joiningSF: StateFunction = {
        case Event(pong: PongReply, Empty) =>
            log.info("Got pong reply, sending find node request")
            routingActor ! AddToBuckets(pong.sender)
            routingActor ! FindNode(selfNode, selfNode.nodeId, local = false)
            stay
        case Event(reply: FindNodeReply, Empty) =>
            log.info("Got find node reply, going into running")
            goto(Running)
    }
}
