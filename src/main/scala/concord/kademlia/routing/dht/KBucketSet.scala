package concord.kademlia.routing.dht

import concord.identity.NodeId
import concord.kademlia.routing.{ActorNode, Node}

import scala.collection.immutable


class KBucketSet[T <: Node](selfNode: ActorNode) {
    self: KBucket.Provider =>

    private val kBucketArray: Array[KBucket[T]] = Array.fill(selfNode.nodeId.size)(newKBucket[T])

    def findClosestK(toId: NodeId, K: Int = kBucketArray(0).capacity) = {
        val indexes = selfNode.nodeId.findNonMatchingFromRight(toId).toStream
        val diff = Stream.range(0, selfNode.nodeId.size, 1).diff(indexes)
        val traversalOrder = indexes ++ diff

        traversalOrder.foldLeft(immutable.List.empty[T]) { (nodes, index) =>
            nodes ++ kBucketArray(index).getNodes.slice(0, K - nodes.size)
        }
    }

    def add(node: T): Boolean = {
        if (isFull(node)) {
            throw new IllegalStateException("KBucket is full!")
        } else {
            if (contains(node)) {
                remove(node)
            }

            getKBucket(node).add(node)
        }
    }

    def isFull(node: T): Boolean = getKBucket(node).isFull

    def contains(node: T): Boolean = getKBucket(node).findNode(node).isDefined

    def remove(node: T): Boolean = getKBucket(node).remove(node)

    private def getKBucket(node: T) = kBucketArray(node.nodeId.size - selfNode.nodeId.longestPrefixLength(node.nodeId) - 1)

}

object KBucketSet {

    trait Provider {
        def newKBucketSet[T <: Node](selfNode: ActorNode, kBucketCapacity: Int) =
            new KBucketSet[T](selfNode) with KBucket.Provider { val capacity = kBucketCapacity }
    }

}
