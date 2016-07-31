package concord.kademlia.routing

import concord.identity.NodeId
import concord.util.time.Clock

import scala.collection.immutable


class KBucket(capacity: Int)(implicit val nodeOrdering: Ordering[TimestampNode]) {
    self: Clock =>

    var nodes = immutable.TreeSet()

    def add(nodeId: NodeId): Boolean = {
        findNode(nodeId) match {
            case Some(existingNode) => updateNodes(nodes - existingNode + TimestampNode(nodeId, getTime))
            case None if size < capacity => updateNodes(nodes + TimestampNode(nodeId, getTime))
            case _ => false
        }
    }

    def findNode(nodeId: NodeId): Option[TimestampNode] = nodes.find(_.nodeId == nodeId)

    def size: Int = nodes.size

    private def updateNodes(op: immutable.TreeSet[TimestampNode]) = {
        nodes = op
        true
    }

}
