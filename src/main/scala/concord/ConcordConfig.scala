package concord

import com.typesafe.config.ConfigFactory
import concord.util.Host


case class ConcordConfig(nodeId: String, systemName: String, host: Host, bucketsCapacity: Int, existingNode: Option[Host])

object ConcordConfig {

    val config = ConfigFactory.load()

    def fromFile: ConcordConfig = ConcordConfig(
        config.getString("concord.identity.nodeId"),
        config.getString("concord.routing.name"),
        Host(config.getString("akka.remote.netty.udp.hostname"), config.getInt("akka.remote.netty.udp.port")),
        config.getInt("concord.routing.bucketsCapacity"),
        Some(Host(config.getString("concord.bootstrap.hostname"), config.getInt("concord.bootstrap.port")))
    )

}
