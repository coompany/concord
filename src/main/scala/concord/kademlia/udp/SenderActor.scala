package concord.kademlia.udp

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString
import concord.kademlia.routing.RoutingMessages._
import concord.kademlia.udp.SenderActor._
import play.api.libs.json._


class SenderActor(parentActor: ActorRef) extends Actor with ActorLogging {

    import context.system
    IO(Udp) ! Udp.SimpleSender

    override def receive: Receive = {
        case Udp.SimpleSenderReady =>
            log.info(s"Sender actor now ready $parentActor")
            parentActor ! SenderReady
            context become ready(sender)
    }

    private def ready(udpSender: ActorRef): Receive = {
        case SenderMessage(remote, message: PingRequest) =>
            udpSender ! getSend(Json.toJson[PingRequest](message), remote)
        case SenderMessage(remote, message: PongReply) =>
            udpSender ! getSend(Json.toJson[PongReply](message), remote)
        case SenderMessage(remote, message: FindNode) =>
            udpSender ! getSend(Json.toJson[FindNode](message), remote)
        case SenderMessage(remote, message: FindNodeReply) =>
            udpSender ! getSend(Json.toJson[FindNodeReply](message), remote)
    }

    private def getSend(message: JsValue, remote: Node): Udp.Send = {
        val toSendMsg = message.as[JsObject] + (recipientJsonKey -> Json.toJson[Node](remote))
        log.info(s"Sending $toSendMsg to $remote")
        Udp.Send(ByteString(toSendMsg.toString), remote.host.toSocketAddress)
    }

}


object SenderActor {

    trait Provider {
        def newSenderActor(parentActor: ActorRef) = Props(new SenderActor(parentActor))
    }

    case object SenderReady
    final case class SenderMessage[T <: Message](remote: Node, message: T)

}
