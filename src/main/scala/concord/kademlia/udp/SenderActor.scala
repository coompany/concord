package concord.kademlia.udp

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString
import concord.kademlia.routing.RoutingMessages._
import concord.kademlia.udp.SenderActor._
import concord.util.Host
import play.api.libs.json.{JsValue, Json}


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
    }

    private def getSend(message: JsValue, remote: Host): Udp.Send = {
        log.info(s"Sending $message to $remote")
        Udp.Send(ByteString(message.toString), remote.toSocketAddress)
    }

}


object SenderActor {

    trait Provider {
        def newSenderActor(parentActor: ActorRef) = Props(new SenderActor(parentActor))
    }

    case object SenderReady
    final case class SenderMessage[T <: Message](remote: Host, message: T)

}
