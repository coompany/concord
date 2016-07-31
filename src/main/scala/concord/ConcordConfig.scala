package concord

import com.typesafe.config.ConfigFactory


case class ConcordConfig(nodeId: String)

case object ConcordConfig {

    val config = ConfigFactory.load

    def fromFile: ConcordConfig = ConcordConfig(
        config.getString("concord.identity.nodeId")
    )

}
