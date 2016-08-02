package concord.kademlia.routing

import akka.actor.ActorRef
import concord.identity.NodeId


object RoutingMessages {

    type Node = ActorNode

    trait Message {
        def sender: Node
    }

    trait Request extends Message
    trait Reply extends Message

    case class PingRequest(sender: Node) extends Request
    case class PongReply(sender: Node) extends Reply

    case class FindNode(sender: Node, searchId: NodeId, local: Boolean) extends Request
    case class FindNodeReply(sender: Node, searchId: NodeId, nodes: List[Node]) extends Reply

    case class NodeRequest(node: ActorRef, request: Request)

    case class AddToBuckets(sender: Node)

}
