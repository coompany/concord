package concord.util

import java.net.InetSocketAddress


case class Host(hostname: String, port: Int) {

    def toAkka(systemName: String, actorName: String): String = s"akka.tcp://$systemName@$hostname:$port/user/$actorName"

    def toSocketAddress = new InetSocketAddress(hostname, port)

    override def equals(obj: scala.Any): Boolean = obj match {
        case host: Host => hostname == host.hostname && port == host.port
        case _ => false
    }

}

object Host {

    def apply(host: String): Host = {
        host.split(":") match {
            case Array(hostname, port) => Host(hostname, port.toInt)
            case _ => throw new IllegalArgumentException(s"Unable to construct host from $host")
        }
    }

}
