package concord.util

import akka.actor.Actor


trait Logging { self: Actor =>

    protected val log = akka.event.Logging(context.system, this)

}
