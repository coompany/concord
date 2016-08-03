package concord.kademlia.store

import akka.actor.{Actor, ActorLogging}
import concord.identity.NodeId
import concord.kademlia.store.StoreActor.{Get, Set}


private[kademlia] class StoreActor[V] extends Actor with ActorLogging {
    this: Store[V] =>

    override def receive = {
        case Get(key) =>
            log.debug(s"Got get request: $key")
            get(key)
        case setMsg: Set[V] =>
            log.debug(s"Got set request: ${setMsg.key} -> ${setMsg.value}")
            set(setMsg.key, setMsg.value)
    }

}

private[kademlia] object StoreActor {

    case class Get(key: NodeId)
    case class Set[V](key: NodeId, value: V)

}
