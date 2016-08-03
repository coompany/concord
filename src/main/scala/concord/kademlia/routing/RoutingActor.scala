package concord.kademlia.routing

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import concord.ConcordConfig
import concord.kademlia.routing.dht.KBucketActor
import concord.kademlia.routing.dht.KBucketMessages.{Add, FindKClosest, FindKClosestReply}
import concord.kademlia.routing.lookup.LookupActor
import concord.kademlia.udp.SenderActor
import concord.kademlia.udp.SenderActor.SenderMessage

import scala.concurrent.duration._
import scala.util.{Failure, Success}


class RoutingActor(selfNode: RemoteNode, senderActor: ActorRef)(implicit val config: ConcordConfig) extends Actor with ActorLogging {
    self: KBucketActor.Provider with LookupActor.Provider =>

    import RoutingMessages._
    import context.dispatcher
    implicit val askTimeout: Timeout = 5 seconds

    private val kBucketActor = context.actorOf(newKBucketActor(selfNode), "kBucketActor")

    override def receive: Receive = {
        case PingRequest(senderNode) =>
            log.info("Got ping request, sending pong reply")
            addToBuckets(senderNode)
            senderActor ! SenderMessage(senderNode.host, PongReply(selfNode))
        case request: FindNode if !request.local =>
            log.info(s"Got lookup find node request against ${request.searchId}")
            addToBuckets(request.sender)
            val lookupActor = context.actorOf(newLookupActor(selfNode, senderActor, kBucketActor, config.bucketsCapacity, config.alpha), "lookupActor")
            lookupActor forward request
        case FindNode(senderNode, searchId, _) =>
            log.info(s"Got local find node request for $searchId")
            addToBuckets(senderNode)
            kBucketActor.ask(FindKClosest(searchId)).onComplete {
                case Success(reply: FindKClosestReply[RemoteNode]) =>
                    senderActor ! SenderMessage(senderNode.host, FindNodeReply(selfNode, reply.searchId, reply.nodes))
                case Failure(exp) => throw exp
            }
            log.info(s"Sent local find node response for $searchId")
        case AddToBuckets(nodeToAdd) =>
            kBucketActor ! Add(nodeToAdd)

    }

    private def addToBuckets(remoteNode: RemoteNode) =
        if (remoteNode.nodeId != selfNode.nodeId)
            kBucketActor forward Add(RemoteNode(remoteNode.host, remoteNode.nodeId))

}

object RoutingActor {

    trait Provider {
        def newRoutingActor(selfNode: RemoteNode, senderActor: ActorRef)(implicit config: ConcordConfig) =
            Props(new RoutingActor(selfNode, senderActor) with SenderActor.Provider with KBucketActor.Provider with LookupActor.Provider)
    }

}
