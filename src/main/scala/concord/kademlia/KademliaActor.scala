package concord.kademlia

import akka.actor.{FSM, Props}
import concord.kademlia.JoiningKadActor.Starting
import concord.kademlia.KademliaActor.{Data, Empty, Running, State}
import concord.kademlia.store.{InMemoryStore, StoreActor}


private[kademlia] object KademliaActor {
    trait State
    case object Running extends State

    trait Data
    case object Empty extends Data
}


class KademliaActor[V] extends FSM[State, Data] {
    
    startWith(Running, Empty)

    private val storeActor = context.system.actorOf(Props(new StoreActor[V] with InMemoryStore[V]))

}


private[kademlia] object JoiningKadActor {
    case object Starting extends State
}

class JoiningKadActor[V] extends KademliaActor[V] {

    startWith(Starting, Empty)

//    when(Starting) {
//        case _ => _
//    }

}
