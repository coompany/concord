package concord.kademlia.routing

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import concord.ConcordConfig
import concord.identity.NodeId
import concord.kademlia.routing.RoutingMessages._
import concord.kademlia.routing.dht.KBucketActor
import concord.kademlia.routing.dht.KBucketMessages._
import concord.kademlia.routing.lookup.LookupActor
import concord.kademlia.routing.lookup.LookupActor.LookupDone
import concord.kademlia.udp.SenderActor
import concord.kademlia.udp.SenderActor.SenderMessage

import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.{Failure, Success}


class RoutingActor(selfNode: RoutingMessages.Node, senderActor: ActorRef)(implicit val config: ConcordConfig) extends Actor with ActorLogging {
    self: KBucketActor.Provider with LookupActor.Provider =>

    import context.dispatcher
    implicit val askTimeout: Timeout = 5 seconds

    private val kBucketActor = context.actorOf(newKBucketActor(selfNode), "kBucketActor")
    private val activeLookups = mutable.Map[NodeId, ActorRef]()

    private def newLookupActor: Props = newLookupActor(selfNode, context.self, senderActor, kBucketActor, config.bucketsCapacity, config.alpha, config.maxRounds)

    override def receive: Receive = {
        case PingRequest(senderNode) =>
            log.info("Got ping request, sending pong reply")
            addToBuckets(senderNode)
            senderActor ! SenderMessage(senderNode, PongReply(selfNode))
        case request: FindNode if !request.local =>
            log.info(s"Got lookup find node request against ${request.searchId}")
            addToBuckets(request.sender)
            val lookupActor = context.actorOf(newLookupActor, "lookupActor")
            activeLookups += request.searchId -> lookupActor
            lookupActor forward request
        case FindNode(senderNode, searchId, _) =>
            log.info(s"Got local find node request for $searchId")
            addToBuckets(senderNode)
            kBucketActor.ask(FindKClosest(searchId)).onComplete {
                case Success(reply: FindKClosestReply[RemoteNode]) =>
                    log.info(s"Sent local find node response for $searchId")
                    senderActor ! SenderMessage(senderNode, FindNodeReply(selfNode, reply.searchId, reply.nodes))
                case Failure(exp) => throw exp
            }
        case reply @ FindNodeReply(sender, searchId, nodes) =>
            activeLookups.get(searchId) match {
                case Some(actor) =>
                    actor ! FindKClosestReply(sender, searchId, nodes)
                case _ => log.info(s"Got find node reply but no active lookup actor\n$reply")
            }
        case LookupDone(searchId) =>
            activeLookups -= searchId
        case AddToBuckets(nodeToAdd) =>
            log.info(s"Adding node $nodeToAdd")
            kBucketActor ! Add(nodeToAdd)

    }

    private def addToBuckets(remoteNode: RemoteNode) =
        if (remoteNode.nodeId != selfNode.nodeId)
            kBucketActor forward Add(RemoteNode(remoteNode.host, remoteNode.nodeId))

}

object RoutingActor {

    trait Provider {
        def newRoutingActor(selfNode: RoutingMessages.Node, senderActor: ActorRef)(implicit config: ConcordConfig) =
            Props(new RoutingActor(selfNode, senderActor) with SenderActor.Provider with KBucketActor.Provider with LookupActor.Provider)
    }

}
