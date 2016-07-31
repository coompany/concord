package concord.kademlia.routing

import concord.identity.NodeId


class KBucketSet(nodeId: NodeId)(implicit val nodeOrdering: Ordering[TimestampNode]) {

    private val kBucketArray = Array.fill()

}
