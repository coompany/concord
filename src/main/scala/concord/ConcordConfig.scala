package concord

import com.typesafe.config.ConfigFactory


case class ConcordConfig(nodeId: String, hostname: String, port: Int)

object ConcordConfig {

    val config = ConfigFactory.load

    def fromFile: ConcordConfig = ConcordConfig(
        config.getString("concord.identity.nodeId"),
        config.getString("akka.remote.netty.udp.hostname"),
        config.getInt("akka.remote.netty.udp.port")
    )

}
