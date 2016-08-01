package concord.kademlia.routing.lookup

import akka.actor.{ActorRef, FSM}
import concord.identity.NodeId
import concord.kademlia.routing.ActorNode
import concord.kademlia.routing.RoutingMessages.{FindNode, FindNodeReply}
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
            kBucketActor ! FindKClosest(request.searchId)
            goto(WaitForLocalNodes) using Lookup(request.sender.nodeId, sender)
    }

    when(WaitForLocalNodes) {
        // when receives response from bucket query remote nodes
        case Event(reply: FindKClosestReply[ActorNode], request: Lookup) =>
            val nodes = selfNode :: reply.nodes
            val qn = QueryNodeData(
                request,
                TreeMap(nodes.map(node => node.nodeId -> NodeQuery(node.ref)): _*)(new request.nodeId.SelfOrder))
            goto(QueryNode) using takeAlphaAndUpdate(qn, alpha)
    }

    when(QueryNode) (remoteReply orElse {
        // start sending requests to nodes to query
        case Event(StartRound, qn: QueryNodeData) =>
            sendRequests(qn.toQuery.values, qn.request.nodeId)
            stay using qn.copy(querying = qn.querying ++ qn.toQuery, toQuery = Map())
        // done querying nodes
        case Event(EndRound, qn: QueryNodeData) => goto(GatherNode) using qn
    })

    when(GatherNode) (remoteReply orElse {
        case Event(StartRound, qn: QueryNodeData) =>
            if (qn.seen.isEmpty) {
                log.warning("All of the nodes are queried, but none of them responded before the round timeout. Try increasing the round timeout.")
                goto(Finalize) using FinalizeData(qn.request, List())
            } else {
                val nextRound = qn.round + 1
                val closest = qn.seen.head

                closest match {
                    case (_, NodeQuery(_, round, _)) if round > qn.round && !qn.lastRound =>
                        goto(QueryNode) using takeAlphaAndUpdate(qn, alpha).copy(round = nextRound)
                    case _ if qn.lastRound =>
                        val kClosest = qn.seen.take(kBucketSize)

                        val kClosestExists = kClosest.exists(Function.tupled((id, node) => !node.replied))

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
            request.sender ! FindNodeReply(selfNode, request.nodeId, kclosest)
            stop()
    }

    onTransition {
        // start timer for query node round
        case _ -> QueryNode => setTimer("queryNodeTimer", StartRound, 1 micro, repeat = false)
    }

    private def sendRequests(toQuery: Iterable[NodeQuery], lookupId: NodeId) = {
        toQuery.foreach { case NodeQuery(ref, _, _) => ref ! FindNode(selfNode, lookupId) }
        setTimer("roundTimer", EndRound, 1 second, repeat = false)
    }

    private def remoteReply: StateFunction = {
        // responds to remote find node requests
        case Event(reply: FindNodeReply, qn: QueryNodeData) =>
            // newly seen nodes in the remote's reply (excluding already seen)
            val newSeen = reply.nodes
                .filterNot(x => qn.seen.exists(_._1 == x.nodeId))
                .map(n => n.nodeId -> NodeQuery(n.ref, round = qn.round + 1))

            val updateQuery = qn.querying - reply.searchId
            val updateSeen = qn.seen ++ newSeen + (reply.searchId -> qn.querying(reply.searchId).copy(replied = true))

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

    private def takeAlphaAndUpdate(qn: QueryNodeData, alpha: Int) = {
        val newToQuery = qn.seen.filter { case (id, data) => data.replied }.take(alpha)
        val newSeen = qn.seen -- qn.toQuery.keys
        qn.copy(toQuery = newToQuery.toMap, seen = newSeen)
    }

    trait Provider {
        def newLookupActor(selfNode: ActorNode, kBucketActor: ActorRef, kBucketSize: Int, alpha: Int) =
            new LookupActor(selfNode, kBucketActor, kBucketSize, alpha)
    }

}
