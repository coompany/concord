package concord.kademlia.routing.dht

import concord.kademlia.routing.{LastSeenOrdering, Node, TimestampNode}
import concord.util.time.Clock

import scala.collection.immutable


class KBucket[T <: Node](val capacity: Int)(implicit val nodeOrdering: Ordering[TimestampNode]) {
    self: Clock =>

    private var nodes = immutable.TreeSet.empty[TimestampNode]

    def add(node: T): Boolean = {
        findNode(node) match {
            case Some(existingNode) => updateNodes(nodes - existingNode + TimestampNode(node, getTime))
            case None if size < capacity => updateNodes(nodes + TimestampNode(node, getTime))
            case _ => false
        }
    }

    def findNode(node: T): Option[TimestampNode] = nodes.find(_.node == node)

    def size: Int = nodes.size

    private def updateNodes(op: immutable.TreeSet[TimestampNode]) = {
        nodes = op
        true
    }

    def getNodes = nodes.map(_.node.asInstanceOf[T])

    def isFull: Boolean = size >= capacity

    def remove(node: T): Boolean = findNode(node) match {
        case Some(timeNode) => updateNodes(nodes - timeNode)
        case _ => false
    }

}

object KBucket {

    trait Provider {
        def capacity: Int
        def newKBucket[T <: Node] = new KBucket[T](capacity)(LastSeenOrdering()) with Clock
    }

}
