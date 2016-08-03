package concord.kademlia.routing

import concord.identity.NodeId
import concord.util.Host
import play.api.libs.json._
import play.api.libs.functional.syntax._


object RoutingMessages {

    type Node = RemoteNode


    // Host writes / reads
    implicit val hostWrites = new Writes[Host] {
        override def writes(o: Host): JsValue = Json.obj(
            "hostname" -> o.hostname,
            "port" -> o.port
        )
    }
    implicit val hostReads: Reads[Host] = (
            (JsPath \ "hostname").read[String] and
            (JsPath \ "port").read[Int]
        )((h, p) => Host(h, p))

    // RemoteNode writes / reads
    implicit val remoteNodeWrites = new Writes[RemoteNode] {
        override def writes(o: RemoteNode): JsValue = Json.obj(
            "host" -> o.host,
            "nodeId" -> o.nodeId.toString
        )
    }
    implicit val remoteNodeReads: Reads[RemoteNode] = (
            (JsPath \ "host").read[Host] and
            (JsPath \ "nodeId").read[String].map(NodeId(_))
        )((h, n) => RemoteNode(h, n))


    trait Message {
        def sender: Node
    }

    trait Request extends Message
    trait Reply extends Message

    case class PingRequest(sender: Node) extends Request
    case class PongReply(sender: Node) extends Reply

    val rpcJsonKey = "rpc"

    // PingRequest writes / reads
    implicit val pingRequestWrites = new Writes[PingRequest] {
        override def writes(o: PingRequest): JsValue = Json.obj(rpcJsonKey -> "ping", "sender" -> o.sender)
    }
    implicit val pingRequestReads: Reads[PingRequest] = (
            (JsPath \ rpcJsonKey).read[String] and
            (JsPath \ "sender").read[Node]
        )((_, s) => PingRequest(s))

    // PongReply writes / reads
    implicit val pongReplyWrites = new Writes[PongReply] {
        override def writes(o: PongReply): JsValue = Json.obj(rpcJsonKey -> "pong", "sender" -> o.sender)
    }
    implicit val pongReplyReads: Reads[PongReply] = (
            (JsPath \ rpcJsonKey).read[String] and
            (JsPath \ "sender").read[Node]
        )((_, s) => PongReply(s))

    case class FindNode(sender: Node, searchId: NodeId, local: Boolean) extends Request
    case class FindNodeReply(sender: Node, searchId: NodeId, nodes: List[Node]) extends Reply

    // FindNode writes / reads
    implicit val findNodeWrites = new Writes[FindNode] {
        override def writes(o: FindNode): JsValue = Json.obj(
            rpcJsonKey -> "find_node",
            "searchId" -> o.searchId.toString,
            "sender" -> o.sender,
            "local" -> o.local
        )
    }
    implicit val findNodeReads: Reads[FindNode] = (
            (JsPath \ rpcJsonKey).read[String] and
            (JsPath \ "searchId").read[String].map(NodeId(_)) and
            (JsPath \ "sender").read[Node] and
            (JsPath \ "local").read[Boolean]
        )((_, id, s, l) => FindNode(s, id, l))

    // FindNodeReply writes / reads
    implicit val findNodeReplyWrites = new Writes[FindNodeReply] {
        override def writes(o: FindNodeReply): JsValue = Json.obj(
            rpcJsonKey -> "find_node_reply",
            "searchId" -> o.searchId.toString,
            "nodes" -> o.nodes,
            "sender" -> o.sender
        )
    }
    implicit val findNodeReplyReads: Reads[FindNodeReply] = (
            (JsPath \ rpcJsonKey).read[String] and
            (JsPath \ "searchId").read[String].map(NodeId(_)) and
            (JsPath \ "nodes").read[List[Node]] and
            (JsPath \ "sender").read[Node]
        )((_, id, n, s) => FindNodeReply(s, id, n))

//    case class NodeRequest(node: Host, request: Request)

    case class AddToBuckets(sender: Node)

}
