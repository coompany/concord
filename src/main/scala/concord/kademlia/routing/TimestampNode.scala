package concord.kademlia.routing

import concord.util.time._


protected[routing] case class TimestampNode(node: Node, timestamp: Epoch)

protected[routing] class LastSeenOrdering extends Ordering[TimestampNode] {
    override def compare(x: TimestampNode, y: TimestampNode): Int = x.timestamp compareTo y.timestamp
}

protected[routing] object LastSeenOrdering {
    def apply(): LastSeenOrdering = new LastSeenOrdering()
}
