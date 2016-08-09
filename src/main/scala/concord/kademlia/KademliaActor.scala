package concord.kademlia

import java.security.KeyPair

import akka.actor.{FSM, Props}
import concord.ConcordConfig
import concord.identity._
import concord.kademlia.KademliaActor._
import concord.kademlia.routing.RoutingMessages._
import concord.kademlia.routing.{RemoteNode, RoutingActor}
import concord.kademlia.udp.ListenerActor.ListenerMessage
import concord.kademlia.udp.SenderActor.{SenderMessage, SenderReady}
import concord.kademlia.udp.{ListenerActor, SenderActor}


object KademliaActor {

    case object Init

    trait State
    case object WaitSender extends State
    case object Running extends State

    trait Data
    case object Empty extends Data

    trait Provider {
        def newKademliaActor[V](nodeId: NodeId, keyPair: KeyPair, puzzle: Puzzle)(implicit config: ConcordConfig) =
            Props(new KademliaActor[V](nodeId, keyPair, puzzle) with SenderActor.Provider with ListenerActor.Provider with RoutingActor.Provider)
    }

    val nodeName = "concordKademlia"

}

class KademliaActor[V](nodeId: NodeId, keyPair: KeyPair, puzzle: Puzzle)(implicit config: ConcordConfig) extends FSM[State, Data] {
    this: SenderActor.Provider with ListenerActor.Provider with RoutingActor.Provider =>

    protected val selfNode = RemoteNode(config.host, nodeId)

    protected val keyDecoder = KeyDecoder(config.identityConfig.keyAlgo)
    protected val signatures = Signatures(keyPair, config.identityConfig.signatureAlgo, keyDecoder)

    protected val listenerActor = context.actorOf(newListenerActor(selfNode, context.self, puzzle.verifyFn(config.identityConfig.c2), signatures), "listenerActor")
    protected val senderActor = context.actorOf(newSenderActor(context.self, signatures), "senderActor")
    protected val routingActor = context.actorOf(newRoutingActor(selfNode, senderActor), "routingActor")

    protected def afterSenderState: State = goto(Running)

    protected def initMessage() = self ! Init

    startWith(WaitSender, Empty)

    when(WaitSender) (waitForSender)

    protected def waitForSender: StateFunction = {
        case Event(SenderReady, _) =>
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
            routingActor forward message
        case Event(request: ListenerMessage, _) =>
            self forward request.message
    }

}


object JoiningKadActor {

    case object PingExisting extends State
    case object Joining extends State

    trait Provider extends KademliaActor.Provider {
        def newJoiningKademliaActor[V](nodeId: NodeId, keyPair: KeyPair, puzzle: Puzzle, existingNode: Node)(implicit config: ConcordConfig) =
            Props(new JoiningKadActor[V](nodeId, keyPair, puzzle, existingNode) with SenderActor.Provider with ListenerActor.Provider with RoutingActor.Provider)
    }

}

class JoiningKadActor[V](nodeId: NodeId, keyPair: KeyPair, puzzle: Puzzle, existingNode: Node)(implicit config: ConcordConfig) extends KademliaActor[V](nodeId, keyPair, puzzle) {
    this: SenderActor.Provider with ListenerActor.Provider with RoutingActor.Provider =>

    import JoiningKadActor._

    override protected def afterSenderState: State = goto(PingExisting)

    when(PingExisting) {
        case Event(Init, Empty) =>
            log.info(s"Connecting to existing Kademlia net via $existingNode")
            self ! existingNode
            stay
        case Event(remoteNode: Node, Empty) =>
            senderActor ! SenderMessage(remoteNode, PingRequest(selfNode))
            goto(Joining)
    }

    when(Joining) (joiningSF orElse remoteReply andThen(x => stay))

    private def joiningSF: StateFunction = {
        case Event(pong: PongReply, _) =>
            log.info("Got pong reply, sending self node lookup and go to running")
            routingActor ! AddToBuckets(pong.sender)
            routingActor ! FindNode(selfNode, selfNode.nodeId, local = false)
            goto(Running)
    }
}
