package concord.identity

import java.net.{InetAddress, NetworkInterface}
import java.security.MessageDigest

import concord.util.time.Clock


class NodeGenerator private (md: MessageDigest) {
    self: Clock =>

    val random = new scala.util.Random

    def generateId(input: Array[Byte]) = {
        md.reset()
        NodeId(md.digest(input))
    }

    def generateId(hostname: String, port: Int): NodeId = {
        val ip = InetAddress.getByName(hostname)
        val net = NetworkInterface.getByInetAddress(ip)
        generateId(net.getHardwareAddress ++ ip.getAddress ++ port.toString.getBytes ++ getTime.toString.getBytes)
    }

}

object NodeGenerator {

    def apply(algorithm: String = "SHA1"): NodeGenerator = new NodeGenerator(MessageDigest.getInstance(algorithm)) with Clock

}
