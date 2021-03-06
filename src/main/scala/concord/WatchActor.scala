package concord

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}


class WatchActor(actors: Seq[ActorRef]) extends Actor with ActorLogging {

    actors foreach context.watch

    override def receive = {
        case Terminated(ref) =>
            log.info(s"Actor $ref has terminated!")
        case event =>
            log.info(s"Got event $event")
    }

}

object WatchActor {

    trait Provider {
        def newWatchActor(actors: ActorRef*) = Props(new WatchActor(actors))
    }

}
