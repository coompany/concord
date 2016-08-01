package concord.kademlia.routing.dht

import concord.identity.NodeId
import concord.kademlia.routing.Node


object KBucketMessages {

    case class FindKClosest(searchId: NodeId)
    case class FindKClosestReply[T <: Node](searchId: NodeId, nodes: List[T])

    case class Add[T <: Node](node: T)

}
