package concord.util


case class Host(hostname: String, port: Int) {

    def toAkka(systemName: String, actorName: String): String = s"akka.udp://$systemName@$hostname:$port/user/$actorName"

}

object Host {

    def apply(host: String): Host = {
        host.split(":") match {
            case Array(hostname, port) => Host(hostname, port.toInt)
            case _ => throw new IllegalArgumentException(s"Unable to construct host from $host")
        }
    }

}
