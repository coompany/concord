package concord.kademlia.routing


object RoutingMessages {

    trait Message {
        def sender: Node
    }

    trait Request extends Message
    trait Reply extends Message

    case class PingRequest(sender: Node) extends Request
    case class PongReply(sender: Node) extends Reply

    case class FindClosest(sender: Node) extends Request
    case class FindClosestReply(sender: Node, nodes: List[Node]) extends Reply

}
