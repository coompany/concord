package concord.identity

import scala.annotation.tailrec


case class NodeId(id: BigInt, size: Int, xNonce: BigInt = BigInt(0)) {

    val byteArray = id.toByteArray match {
        case x if x.length != size => x.takeRight(size / 8)
        case x => x
    }

    override def toString = str

    lazy val str = {
        val binary = id.toString(2)
        val zeroPad = (for (i <- 1 to size - binary.length()) yield '0')(collection.breakOut)
        zeroPad + binary
    }

    override def equals(obj: scala.Any): Boolean = obj match {
        case that: NodeId => size == that.size && id == that.id
        case _ => false
    }

    def findNonMatchingFromRight(other: NodeId) = foldRight(List.empty[Int]) {
        case (bit, list) => if (isBitSet(bit) != other.isBitSet(bit)) (bit - 1) :: list else list
    }

    def isBitSet(bit: Int) = id.testBit(bit - 1)

    def distance(other: NodeId) = NodeId(id ^ other.id, size)

    def longestPrefixLength(node: NodeId): Int = {
        val dist = distance(node)
        @tailrec def _longestPrefixLength(bit: Int): Int = bit match {
            case 0 => dist.size
            case _ if dist.isBitSet(bit) => dist.size - bit
            case _ => _longestPrefixLength(bit - 1)
        }
        _longestPrefixLength(dist.size)
    }

    private def foldRight[T](acc: T, start: Int = 1)(op: (Int, T) => T): T = {
        @tailrec def _foldRight(bit: Int, acc: T): T = if (bit == size) acc else _foldRight(bit + 1, op(bit, acc))
        _foldRight(start, acc)
    }

    class SelfOrder extends Ordering[NodeId] { // closeness relative to "this" id

        override def compare(x: NodeId, y: NodeId) = {
            val xDistInt = distance(x).id
            val yDistInt = distance(y).id

            xDistInt.compare(yDistInt)
        }
    }

}

case object NodeId {

    def apply(bitStr: String): NodeId = {
        val (decVal, _) = bitStr.foldRight((BigInt(0), 0)) {
            case (c, (sum, index)) if c == '1' => (sum.setBit(index), index + 1)
            case (c, (sum, index)) if c == '0' => (sum, index + 1)
        }

        NodeId(decVal, bitStr.length())
    }

    private def toUnsigned(bytes: Array[Byte]): BigInt = {
        def _toUnsigned(index: Int = 0, decVal: BigInt = BigInt(0)): BigInt =
            if (index == bytes.length) decVal else _toUnsigned(index + 1, (decVal << 8) + (bytes(index) & 0xff))
        _toUnsigned()
    }

    def apply(byteArray: Array[Byte], nonce: BigInt): NodeId = {
        val decVal = toUnsigned(byteArray)
        NodeId(decVal, byteArray.length * 8, nonce)
    }

    def apply(byteArray: Array[Byte]): NodeId = NodeId(byteArray, BigInt(0))

}
