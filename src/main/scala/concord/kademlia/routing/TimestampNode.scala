package concord.kademlia.routing

import concord.identity.NodeId
import concord.util.time._


protected[routing] case class TimestampNode(nodeId: NodeId, timestamp: Epoch)

protected[routing] class LastSeenOrdering extends Ordering[TimestampNode] {
    override def compare(x: TimestampNode, y: TimestampNode): Int = x.timestamp compareTo y.timestamp
}
