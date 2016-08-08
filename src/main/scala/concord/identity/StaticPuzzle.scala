package concord.identity

import java.security.{KeyPair, KeyPairGenerator, MessageDigest}

import scala.annotation.tailrec


class StaticPuzzle(hashAlgo: String, c1: Int) {

    private val digest = MessageDigest.getInstance(hashAlgo)

    def findPair(algorithm: String): (KeyPair, NodeId) = findPair(KeyPairGenerator.getInstance(algorithm))

    @tailrec
    private def findPair(generator: KeyPairGenerator): (KeyPair, NodeId) = {
        val pair = generator.generateKeyPair()
        val h1 = digest.digest(pair.getPublic.getEncoded)
        val p = NodeId(digest.digest(h1))

        if (testP(p)) (pair, NodeId(h1))
        else findPair(generator)
    }

    private def testP(p: NodeId): Boolean = {
        for (i <- p.size until p.size - c1 by -1) {
            if (p.isBitSet(i)) return false
        }
        true
    }

    def verifySolution(nodeId: NodeId): Boolean = testP(NodeId(digest.digest(nodeId.byteArray)))

}


object StaticPuzzle {

    def apply(hashAlgo: String, c1: Int): StaticPuzzle = new StaticPuzzle(hashAlgo, c1)

}
