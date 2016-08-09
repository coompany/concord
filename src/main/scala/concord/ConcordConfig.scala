package concord

import com.typesafe.config.{Config, ConfigFactory}
import concord.identity.NodeId
import concord.kademlia.routing.RemoteNode
import concord.kademlia.routing.RoutingMessages.Node
import concord.util.{Host, CaseClassPrinter}


case class ConcordConfig(systemName: String,
                         host: Host,
                         bucketsCapacity: Int,
                         existingNode: Option[Node],
                         alpha: Int,
                         maxRounds: Int,
                         identityConfig: IdentityConfig) extends CaseClassPrinter {

    override def toString(indent: String): String = prettyStr[ConcordConfig](indent)

}

object ConcordConfig {

    val config = ConfigFactory.load()
    val bootstrapConfig = config.getConfig("concord.bootstrap")

    val joining = bootstrapConfig.getBoolean("joining")

    val bootstrapHost = joining match {
        case true => Some(
            RemoteNode(
                Host(bootstrapConfig.getString("hostname"), bootstrapConfig.getInt("port")),
                NodeId(bootstrapConfig.getString("nodeId"), bootstrapConfig.getString("nonce"))
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


case class IdentityConfig(public: String,
                          secret: String,
                          xnonce: String,
                          keyAlgo: String,
                          hashAlgo: String,
                          c1: Int, c2: Int) extends CaseClassPrinter {

    override def toString(indent: String): String = prettyStr[IdentityConfig](indent)

}

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
