package concord.kademlia.udp

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString
import concord.identity.{Signature, Signatures}
import concord.kademlia.routing.RoutingMessages._
import concord.kademlia.udp.SenderActor._
import play.api.libs.json._


class SenderActor(parentActor: ActorRef, signatures: Signatures) extends Actor with ActorLogging {

    import context.system
    IO(Udp) ! Udp.SimpleSender

    override def receive: Receive = {
        case Udp.SimpleSenderReady =>
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

    import signatures._
    private def getSend(message: JsValue, remote: Node): Udp.Send = {
        val toSendMsg = message.as[JsObject] + (recipientJsonKey -> Json.toJson[Node](remote))
        val signature = signatures.signMessage(toSendMsg.toString)
        val signedMsg = toSendMsg + (signatureJsonKey -> Json.toJson[Signature](signature))
        Udp.Send(ByteString(signedMsg.toString), remote.host.toSocketAddress)
    }

}


object SenderActor {

    trait Provider {
        def newSenderActor(parentActor: ActorRef, signatures: Signatures) = Props(new SenderActor(parentActor, signatures))
    }

    case object SenderReady
    final case class SenderMessage[T <: Message](remote: Node, message: T)

}
