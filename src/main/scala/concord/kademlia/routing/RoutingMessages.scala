package concord.kademlia.routing

import concord.identity.NodeId


object RoutingMessages {

    trait Message {
        def sender: NodeId
    }

    trait Request extends Message
    trait Reply extends Message

    case class PingRequest(sender: NodeId) extends Request
    case class PongReply(sender: NodeId) extends Reply

    case class FindClosest(sender: NodeId) extends Request
    case class FindClosestReply(sender: NodeId, nodes: List[Node]) extends Reply

}
