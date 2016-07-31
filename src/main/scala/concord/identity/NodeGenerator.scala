package concord.identity

import java.security.{MessageDigest, SecureRandom}

import concord.util.time.Clock

import scala.util.Random


class NodeGenerator private (md: MessageDigest) {
    self: Clock =>

    val random = new scala.util.Random

    def generateId(input: Array[Byte]) = {
        md.reset()
        NodeId(md.digest(input))
    }

    def generateId: NodeId = generateId((getTime & Random.nextLong()).toString.getBytes())

}

object NodeGenerator {

    def apply(algorithm: String = "SHA1"): NodeGenerator = new NodeGenerator(MessageDigest.getInstance(algorithm)) with Clock

}
