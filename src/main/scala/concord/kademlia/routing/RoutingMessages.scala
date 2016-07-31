package concord.kademlia.routing

import concord.identity.NodeId


object RoutingMessages {

    trait Message

    trait Request extends Message
    trait Reply extends Message

    case object PingRequest extends Request
    case object PongReply extends Reply

    case class FindClosest(nodeId: NodeId) extends Request
    case class FindClosestReply(nodeId: NodeId, nodes: List[Node]) extends Reply

}
