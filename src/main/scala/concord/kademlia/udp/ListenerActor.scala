package concord.kademlia.udp

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Udp}
import concord.kademlia.routing.RoutingMessages._
import concord.kademlia.udp.ListenerActor.ListenerMessage
import concord.util.Host
import play.api.libs.json._


class ListenerActor(selfNode: Node, parentActor: ActorRef) extends Actor with ActorLogging {

    import context.system
    IO(Udp) ! Udp.Bind(self, selfNode.host.toSocketAddress)

    override def receive = {
        case Udp.Bound(local) =>
            context.become(ready(sender()))
    }

    private def ready(socket: ActorRef): Receive = {
        case Udp.Received(data, remote) =>
            handleRemoteDestination(Json.parse(data.toArray), remote)
        case Udp.Unbind  => socket ! Udp.Unbind
        case Udp.Unbound => context.stop(self)
    }

    private def handleRemoteDestination(implicit json: JsValue, remote: InetSocketAddress) = (json \ recipientJsonKey).as[Node] match {
        case remoteNode: Node if remoteNode.nodeId == selfNode.nodeId => handleRpcType
        case remoteNode: Node => log.info(s"Got request addressed to wrong nodeId $remoteNode")
        case _ => log.warning(s"Recipient not found in message!\n$json")
    }

    private def handleRpcType(implicit json: JsValue, remote: InetSocketAddress) = (json \ rpcJsonKey).as[String] match {
        case "ping" =>
            handleJsonParsing(json.validate[PingRequest])
        case "pong" =>
            handleJsonParsing(json.validate[PongReply])
        case "find_node" =>
            handleJsonParsing(json.validate[FindNode])
        case "find_node_reply" =>
            handleJsonParsing(json.validate[FindNodeReply])
    }

    private def handleJsonParsing[T <: Message](result: JsResult[T])(implicit json: JsValue, remote: InetSocketAddress) = result match {
        case JsSuccess(request, _) =>
            parentActor ! ListenerMessage(Host(remote.getHostName, remote.getPort), request)
        case JsError(errors) =>
            val errorStr = errors.map(s => s"${s._1}:\n\t${s._2.mkString("\n\t")}").mkString("\n")
            log.error(s"Unable to parse request from $json\nwith errors:\n$errorStr")
    }

}


object ListenerActor {

    trait Provider {
        def newListenerActor(selfNode: Node, parentActor: ActorRef) = Props(new ListenerActor(selfNode, parentActor))
    }

    final case class ListenerMessage(remote: Host, message: Message)

}
