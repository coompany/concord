package concord.kademlia.udp

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Udp}
import concord.util.Host
import concord.kademlia.routing.RoutingMessages._
import concord.kademlia.udp.ListenerActor.ListenerMessage
import play.api.libs.json._


class ListenerActor(host: Host, parentActor: ActorRef) extends Actor with ActorLogging {

    import context.system
    IO(Udp) ! Udp.Bind(self, host.toSocketAddress)

    override def receive = {
        case Udp.Bound(local) =>
            context.become(ready(sender()))
    }

    private def ready(socket: ActorRef): Receive = {
        case Udp.Received(data, remote) =>
            implicit val json = Json.parse(data.toArray)
            implicit val r = remote
            handleRpcType
        case Udp.Unbind  => socket ! Udp.Unbind
        case Udp.Unbound => context.stop(self)
    }

    private def handleRpcType(implicit json: JsValue, remote: InetSocketAddress) = (json \ rpcJsonKey).as[String] match {
        case "ping" =>
            log.info(s"Got ping request from $remote: $json")
            handleJsonParsing(json.validate[PingRequest])
        case "pong" =>
            log.info(s"Got pong reply from $remote: $json")
            handleJsonParsing(json.validate[PongReply])
        case "find_node" =>
            log.info(s"Got find node from $remote: $json")
            handleJsonParsing(json.validate[FindNode])
    }

    private def handleJsonParsing[T <: Message](result: JsResult[T])(implicit json: JsValue, remote: InetSocketAddress) = result match {
//        case JsSuccess(request: Request, _) =>
//            parentActor ! NodeRequest(request.sender.host, request)
        case JsSuccess(request, _) =>
            parentActor ! ListenerMessage(Host(remote.getHostName, remote.getPort), request)
        case JsError(errors) =>
            val errorStr = errors.map(s => s"${s._1}:\n\t${s._2.mkString("\n\t")}").mkString("\n")
            log.error(s"Unable to parse request from $json\nwith errors:\n$errorStr")
    }

}


object ListenerActor {

    trait Provider {
        def newListenerActor(host: Host, parentActor: ActorRef) = Props(new ListenerActor(host, parentActor))
    }

    final case class ListenerMessage(remote: Host, message: Message)

}
