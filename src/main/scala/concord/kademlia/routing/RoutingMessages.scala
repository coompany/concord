package concord.kademlia.routing

import concord.identity.NodeId
import concord.util.Host
import play.api.libs.functional.syntax._
import play.api.libs.json._


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

    // NodeId writes / reads
    implicit val nodeIdWrites = new Writes[NodeId] {
        override def writes(o: NodeId): JsValue = Json.obj(
            "id" -> o.toBitString,
            "nonce" -> o.nonce.toString
        )
    }
    implicit val nodeIdReads: Reads[NodeId] = (
            (JsPath \ "id").read[String] and
            (JsPath \ "nonce").read[String]
        )((id, nonce) => NodeId(id, nonce))

    // RemoteNode writes / reads
    implicit val remoteNodeWrites = new Writes[RemoteNode] {
        override def writes(o: RemoteNode): JsValue = Json.obj(
            "host" -> o.host,
            "nodeId" -> o.nodeId
        )
    }
    implicit val remoteNodeReads: Reads[RemoteNode] = (
            (JsPath \ "host").read[Host] and
            (JsPath \ "nodeId").read[NodeId]
        )((h, n) => RemoteNode(h, n))


    trait Message {
        def sender: Node
    }

    trait Request extends Message
    trait Reply extends Message

    case class PingRequest(sender: Node) extends Request
    case class PongReply(sender: Node) extends Reply

    val rpcJsonKey = "rpc"
    val senderJsonKey = "sender"
    val recipientJsonKey = "recipient"      // used in sender and listener actors
    val signatureJsonKey = "signature"

    // PingRequest writes / reads
    implicit val pingRequestWrites = new Writes[PingRequest] {
        override def writes(o: PingRequest): JsValue = Json.obj(rpcJsonKey -> "ping", senderJsonKey -> o.sender)
    }
    implicit val pingRequestReads: Reads[PingRequest] = (
            (JsPath \ rpcJsonKey).read[String] and
            (JsPath \ senderJsonKey).read[Node]
        )((_, s) => PingRequest(s))

    // PongReply writes / reads
    implicit val pongReplyWrites = new Writes[PongReply] {
        override def writes(o: PongReply): JsValue = Json.obj(rpcJsonKey -> "pong", senderJsonKey -> o.sender)
    }
    implicit val pongReplyReads: Reads[PongReply] = (
            (JsPath \ rpcJsonKey).read[String] and
            (JsPath \ senderJsonKey).read[Node]
        )((_, s) => PongReply(s))

    case class FindNode(sender: Node, searchId: NodeId, local: Boolean) extends Request
    case class FindNodeReply(sender: Node, searchId: NodeId, nodes: List[Node]) extends Reply

    // FindNode writes / reads
    implicit val findNodeWrites = new Writes[FindNode] {
        override def writes(o: FindNode): JsValue = Json.obj(
            rpcJsonKey -> "find_node",
            "searchId" -> o.searchId,
            senderJsonKey -> o.sender,
            "local" -> o.local
        )
    }
    implicit val findNodeReads: Reads[FindNode] = (
            (JsPath \ rpcJsonKey).read[String] and
            (JsPath \ "searchId").read[NodeId] and
            (JsPath \ senderJsonKey).read[Node] and
            (JsPath \ "local").read[Boolean]
        )((_, id, s, l) => FindNode(s, id, l))

    // FindNodeReply writes / reads
    implicit val findNodeReplyWrites = new Writes[FindNodeReply] {
        override def writes(o: FindNodeReply): JsValue = Json.obj(
            rpcJsonKey -> "find_node_reply",
            "searchId" -> o.searchId,
            "nodes" -> o.nodes,
            senderJsonKey -> o.sender
        )
    }
    implicit val findNodeReplyReads: Reads[FindNodeReply] = (
            (JsPath \ rpcJsonKey).read[String] and
            (JsPath \ "searchId").read[NodeId] and
            (JsPath \ "nodes").read[List[Node]] and
            (JsPath \ senderJsonKey).read[Node]
        )((_, id, n, s) => FindNodeReply(s, id, n))

    case class AddToBuckets(sender: Node)

}
