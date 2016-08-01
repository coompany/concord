package concord.util

import akka.actor.Actor
import org.slf4j.LoggerFactory


trait LoggingActor { self: Actor =>

    protected val log = akka.event.Logging(context.system, this)

}

trait Logging {

    protected val log = LoggerFactory.getLogger(this.getClass)

}
