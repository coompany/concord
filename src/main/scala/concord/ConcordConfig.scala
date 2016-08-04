package concord

import com.typesafe.config.ConfigFactory
import concord.identity.NodeId
import concord.kademlia.routing.RemoteNode
import concord.kademlia.routing.RoutingMessages.Node
import concord.util.Host


case class ConcordConfig(nodeId: String,
                         systemName: String,
                         host: Host,
                         bucketsCapacity: Int,
                         existingNode: Option[Node],
                         alpha: Int,
                         maxRounds: Int)

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
        config.getString("concord.identity.nodeId"),
        config.getString("concord.routing.name"),
        Host(config.getString("concord.hostname"), config.getInt("concord.port")),
        config.getInt("concord.routing.bucketsCapacity"),
        bootstrapHost,
        config.getInt("concord.routing.alpha"),
        config.getInt("concord.routing.maxRounds")
    )

}
