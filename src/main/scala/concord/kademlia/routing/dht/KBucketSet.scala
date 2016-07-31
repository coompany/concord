package concord.kademlia.routing.dht

import concord.identity.NodeId
import concord.kademlia.routing.Node

import scala.collection.immutable


class KBucketSet[T <: Node](nodeId: NodeId) {
    self: KBucket.Provider =>

    private val kBucketArray: Array[KBucket[T]] = Array.fill(nodeId.size)(newKBucket[T])

    def findClosestK(toId: NodeId, K: Int = kBucketArray(0).capacity) = {
        val indexes = nodeId.findNonMatchingFromRight(toId).toStream
        val diff = Stream.range(0, nodeId.size, 1).diff(indexes)
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

    private def getKBucket(node: T) = kBucketArray(node.nodeId.size - nodeId.longestPrefixLength(node.nodeId) - 1)

}

object KBucketSet {

    trait Provider {
        def newKBucketSet[T <: Node](nodeId: NodeId, kBucketCapacity: Int) =
            new KBucketSet[T](nodeId) with KBucket.Provider { val capacity = kBucketCapacity }
    }

}
