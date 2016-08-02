package concord.kademlia.routing.lookup

import akka.actor.{ActorRef, FSM}
import concord.identity.NodeId
import concord.kademlia.routing.ActorNode
import concord.kademlia.routing.RoutingMessages.{FindNode, FindNodeReply, NodeRequest}
import concord.kademlia.routing.dht.KBucketMessages.{FindKClosest, FindKClosestReply}
import concord.kademlia.routing.lookup.LookupActor.{Data, State}

import scala.collection.SortedMap
import scala.collection.immutable.TreeMap
import scala.concurrent.duration._


class LookupActor(selfNode: ActorNode, kBucketActor: ActorRef, kBucketSize: Int, alpha: Int) extends FSM[State, Data] {

    import LookupActor._

    startWith(Initial, Empty)

    when(Initial) {
        // when requested to find a node asks to bucket first
        case Event(request: FindNode, _) =>
            log.info("Got find node request in lookup FSM")
            kBucketActor ! FindKClosest(request.searchId)
            goto(WaitForLocalNodes) using Lookup(request.sender.nodeId, sender)
    }

    when(WaitForLocalNodes) {
        // when receives response from bucket query remote nodes
        case Event(reply: FindKClosestReply[ActorNode], request: Lookup) =>
            log.info("Got K closest reply")
            val nodes = selfNode :: reply.nodes
            val qn = QueryNodeData(request,
                TreeMap(nodes.map(node => node.nodeId -> NodeQuery(node.ref)): _*)(new request.nodeId.SelfOrder))
            goto(QueryNode) using takeAlphaAndUpdate(qn, alpha)
    }

    when(QueryNode) (remoteReply orElse {
        // start sending requests to nodes to query
        case Event(StartRound, qn: QueryNodeData) =>
            log.info(s"Sending requests for round ${qn.round}")
            sendRequests(qn.toQuery.values, qn.request.nodeId)
            stay using qn.copy(querying = qn.querying ++ qn.toQuery, toQuery = Map())
        // done querying nodes
        case Event(EndRound, qn: QueryNodeData) => goto(GatherNode) using qn
    })

    when(GatherNode) (remoteReply orElse {
        case Event(StartRound, qn: QueryNodeData) =>
            log.info(s"Gathering nodes info for ${qn.seen}")
            if (qn.seen.isEmpty) {
                log.warning("All of the nodes are queried, but none of them responded before the round timeout. Try increasing the round timeout.")
                goto(Finalize) using FinalizeData(qn.request, List())
            } else {
                val nextRound = qn.round + 1
                val closest = qn.seen.head

                closest match {
                    case (_, NodeQuery(_, round, _)) if round > qn.round && !qn.lastRound =>
                        log.info(s"Not last round $round - ${qn.lastRound}, going back to query node state")
                        goto(QueryNode) using takeAlphaAndUpdate(qn, alpha).copy(round = nextRound)
                    case _ if qn.lastRound =>
                        val kClosest = qn.seen.take(kBucketSize)

                        val kClosestExists = kClosest.exists(Function.tupled((id, node) => !node.replied))

                        log.info(s"Last round: $kClosest - $kClosestExists")

                        kClosestExists match { // keep querying since not all of the previous request responded
                            case true => goto(QueryNode) using takeAlphaAndUpdate(qn, kBucketSize).copy(round = nextRound, lastRound = true)
                            case false => goto(Finalize) using FinalizeData(qn.request, kClosest.toList.map(Function.tupled((id, node) => ActorNode(node.ref, id))))
                        }
                    case _ =>
                        goto(QueryNode) using takeAlphaAndUpdate(qn, kBucketSize).copy(round = nextRound, lastRound = true)
                }
            }
    })

    when(Finalize) {
        case Event(StartRound, FinalizeData(request, kclosest)) =>
            log.info(s"Finalizing node lookup, sending reply back to sender")
            request.sender ! FindNodeReply(selfNode, request.nodeId, kclosest)
            stop()
    }

    onTransition {
        // start transition timers
        case _ -> QueryNode => initStateTimer("startQueryNode")
        case QueryNode -> GatherNode => initStateTimer("startGatherNode")
        case GatherNode -> Finalize => initStateTimer("startFinalize")
    }

    private def initStateTimer(name: String) = setTimer(name, StartRound, 1 micro, repeat = false)

    private def takeAlphaAndUpdate(qn: QueryNodeData, alpha: Int) = {
        val newToQuery = qn.seen.filterNot(_._2.replied).take(alpha)
        val newSeen = qn.seen -- qn.toQuery.keys
        log.info(s"New to query nodes: $newToQuery")
        qn.copy(toQuery = newToQuery.toMap, seen = newSeen)
    }

    private def sendRequests(toQuery: Iterable[NodeQuery], lookupId: NodeId) = {
        toQuery.foreach {
            case NodeQuery(ref, round, _) =>
                log.info(s"Sending find node request for $ref at round $round")
                ref ! NodeRequest(selfNode.ref, FindNode(selfNode, lookupId, local = true))
        }
        setTimer("roundTimer", EndRound, 1 second, repeat = false)
    }

    private def remoteReply: StateFunction = {
        // responds to remote find node requests
        case Event(reply: FindKClosestReply[ActorNode], qn: QueryNodeData) =>
            log.info(s"Got find node reply for ${reply.searchId}")
            // newly seen nodes in the remote's reply (excluding already seen)
            val newSeen = reply.nodes
                .filterNot(x => qn.seen.exists(_._1 == x.nodeId))
                .map(n => n.nodeId -> NodeQuery(n.ref, round = qn.round + 1))

            val updateQuery = qn.querying - reply.searchId
            val updateSeen = qn.seen ++ newSeen + (reply.sender.nodeId -> qn.querying(reply.sender.nodeId).copy(replied = true))

            stay using qn.copy(seen = updateSeen, querying = updateQuery)
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
    case class QueryNodeData(request: Lookup,
                             seen: SortedMap[NodeId, NodeQuery],
                             toQuery: Map[NodeId, NodeQuery] = Map(),
                             querying: Map[NodeId, NodeQuery] = Map(),
                             round: Int = 1,
                             lastRound: Boolean = false) extends Data
    case class NodeQuery(ref: ActorRef, round: Int = 1, replied: Boolean = false)
    case class FinalizeData(request: Lookup, kClosest: List[ActorNode]) extends Data

    case object StartRound
    case object EndRound

    trait Provider {
        def newLookupActor(selfNode: ActorNode, kBucketActor: ActorRef, kBucketSize: Int, alpha: Int) =
            new LookupActor(selfNode, kBucketActor, kBucketSize, alpha)
    }

}
