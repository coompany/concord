package concord.util

import akka.actor.Actor
import akka.event.Logging


trait Logging { self: Actor =>

    protected val log = Logging(context.system, this)

}
