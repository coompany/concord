package concord

import akka.actor.ActorSystem
import concord.identity.{KeyDecoder, Puzzle}
import concord.kademlia.{JoiningKadActor, KademliaActor}
import concord.util.Logging


class Concord(config: ConcordConfig) extends Logging {
    self: JoiningKadActor.Provider with WatchActor.Provider =>

    private val actorSystem = ActorSystem(config.systemName)

    private val puzzle = Puzzle(config.identityConfig.hashAlgo, config.identityConfig.c1)

    val (keyPair, nodeId) = config.identityConfig.public match {
        case s if s.isEmpty => puzzle.newId(config.identityConfig.keyAlgo, config.identityConfig.c2)
        case pubKey: String =>
            val keys = KeyDecoder.keyPair(pubKey, config.identityConfig.secret, config.identityConfig.keyAlgo)
            val node = puzzle.pkNonceToNode(keys.getPublic, BigInt(config.identityConfig.xnonce))
            if (puzzle.verify(node, config.identityConfig.c2)) {
                (keys, node)
            } else {
                throw new RuntimeException(s"Submitted public key is not valid for current identity parameters:\nPK: $pubKey")
            }
    }

    private val kadNode = config.existingNode match {
        case Some(node) => actorSystem.actorOf(newJoiningKademliaActor[Int](nodeId, keyPair, puzzle, node)(config), KademliaActor.nodeName)
        case _ => actorSystem.actorOf(newKademliaActor[Int](nodeId, keyPair, puzzle)(config), KademliaActor.nodeName)
    }

    actorSystem.actorOf(newWatchActor(kadNode), "watcherActor")

    log.info(s"\n\nStarting Concord with following info:\n\n$config\n\n$nodeId\n\n")

}


object Concord {

    def apply(config: ConcordConfig = ConcordConfig.fromFile): Concord =
        new Concord(config) with JoiningKadActor.Provider with WatchActor.Provider

}
