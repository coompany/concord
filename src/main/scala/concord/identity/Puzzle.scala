package concord.identity

import java.security._

import scala.annotation.tailrec


class Puzzle(hashAlgo: String, c1: Int) {

    private[identity] val digest = MessageDigest.getInstance(hashAlgo)

    private val random = SecureRandom.getInstance("SHA1PRNG")

    def findPair(algorithm: String): KeyPair = findPair(KeyPairGenerator.getInstance(algorithm))

    @tailrec
    private def findPair(generator: KeyPairGenerator): KeyPair = {
        val pair = generator.generateKeyPair()
        val h1 = digest.digest(pair.getPublic.getEncoded)
        val p = NodeId(digest.digest(h1))

        if (testP(p, c1)) pair
        else findPair(generator)
    }

    @tailrec
    final def findX(pk: PublicKey, c2: Int): BigInt  = {
        val nodeId = NodeId(digest.digest(pk.getEncoded))
        val x = BigInt(nodeId.size, random)
        val p = NodeId(digest.digest((nodeId.id ^ x).toByteArray))
        if (testP(p, c2)) x
        else findX(pk, c2)
    }

    private def testP(p: NodeId, c: Int): Boolean = {
        for (i <- p.size until p.size - c by -1) {
            if (p.isBitSet(i)) return false
        }
        true
    }

    def pkNonceToNode(pk: PublicKey, nonce: BigInt): NodeId = NodeId(digest.digest(pk.getEncoded), nonce)

    def verifyStatic(nodeId: NodeId): Boolean = testP(NodeId(digest.digest(nodeId.byteArray)), c1)

    def verifyDynamic(nodeId: NodeId, c2: Int): Boolean = testP(NodeId(digest.digest((nodeId.id ^ nodeId.nonce).toByteArray)), c2)

    def verify(nodeId: NodeId, c2: Int): Boolean = verifyStatic(nodeId) && verifyDynamic(nodeId, c2)

    def newId(keyAlgo: String, c2: Int): (KeyPair, NodeId) = {
        val pair = findPair(keyAlgo)
        val nonce = findX(pair.getPublic, c2)
        (pair, pkNonceToNode(pair.getPublic, nonce))
    }

}


object Puzzle {

    def apply(hashAlgo: String, c1: Int): Puzzle = new Puzzle(hashAlgo, c1)

}
