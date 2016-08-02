package concord

import com.typesafe.config.ConfigFactory
import concord.util.Host


case class ConcordConfig(nodeId: String,
                         systemName: String,
                         host: Host,
                         bucketsCapacity: Int,
                         existingNode: Option[Host],
                         alpha: Int)

object ConcordConfig {

    val config = ConfigFactory.load()

    val joining = config.getBoolean("concord.bootstrap.joining")

    val bootstrapHost = joining match {
        case true => Some(Host(config.getString("concord.bootstrap.hostname"), config.getInt("concord.bootstrap.port")))
        case false => None
    }

    def fromFile: ConcordConfig = ConcordConfig(
        config.getString("concord.identity.nodeId"),
        config.getString("concord.routing.name"),
        Host(config.getString("concord.hostname"), config.getInt("concord.port")),
        config.getInt("concord.routing.bucketsCapacity"),
        bootstrapHost,
        config.getInt("concord.routing.alpha")
    )

}
