package concord

import akka.actor.{Actor, ActorRef, Terminated}
import concord.util.LoggingActor


class WatchActor(actors: Seq[ActorRef]) extends Actor with LoggingActor {

    actors foreach context.watch

    override def receive = {
        case Terminated(ref) =>
            log.info(s"Actor $ref has terminated!")
    }

}

object WatchActor {

    trait Provider {
        def newWatchActor(actors: ActorRef*) = new WatchActor(actors)
    }

}
