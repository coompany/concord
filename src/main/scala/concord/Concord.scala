package concord

import akka.actor.{ActorSystem, Props}
import concord.identity.NodeId
import concord.kademlia.KademliaActor
import org.slf4j.LoggerFactory


class Concord(config: ConcordConfig) {

    val log = LoggerFactory.getLogger(getClass)

    val nodeId = config.nodeId match {
        case s if s.isEmpty => NodeId()
        case s: String => NodeId(s)
    }

    private val actorSystem = ActorSystem("concord-actor-system")

    private val kadNode = actorSystem.actorOf(Props(new KademliaActor))

}


object Concord {

    def apply(config: ConcordConfig = ConcordConfig.fromFile): Concord = new Concord(config)

}
