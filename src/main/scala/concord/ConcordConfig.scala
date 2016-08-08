package concord

import com.typesafe.config.{Config, ConfigFactory}
import concord.identity.NodeId
import concord.kademlia.routing.RemoteNode
import concord.kademlia.routing.RoutingMessages.Node
import concord.util.Host


case class ConcordConfig(systemName: String,
                         host: Host,
                         bucketsCapacity: Int,
                         existingNode: Option[Node],
                         alpha: Int,
                         maxRounds: Int,
                         identityConfig: IdentityConfig)

object ConcordConfig {

    val config = ConfigFactory.load()

    val joining = config.getBoolean("concord.bootstrap.joining")

    val bootstrapHost = joining match {
        case true => Some(
            RemoteNode(
                Host(config.getString("concord.bootstrap.hostname"), config.getInt("concord.bootstrap.port")),
                NodeId(config.getString("concord.bootstrap.nodeId"))
            ))
        case false => None
    }

    def fromFile: ConcordConfig = ConcordConfig(
        config.getString("concord.routing.name"),
        Host(config.getString("concord.hostname"), config.getInt("concord.port")),
        config.getInt("concord.routing.bucketsCapacity"),
        bootstrapHost,
        config.getInt("concord.routing.alpha"),
        config.getInt("concord.routing.maxRounds"),
        IdentityConfig.fromFile(config.getConfig("concord.identity"))
    )

}


case class IdentityConfig(public: String, secret: String, xnonce: String, keyAlgo: String, hashAlgo: String, c1: Int, c2: Int)

object IdentityConfig {

    def fromFile(config: Config) = IdentityConfig(
        config.getString("public"),
        config.getString("secret"),
        config.getString("xnonce"),
        config.getString("algorithms.keys"),
        config.getString("algorithms.hash"),
        config.getInt("c1"), config.getInt("c2")
    )

}
