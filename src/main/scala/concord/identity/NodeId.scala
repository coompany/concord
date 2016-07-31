package concord.identity


case class NodeId(id: BigInt, size: Int) {

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
}

case object NodeId {

    private val nodeGenerator: NodeGenerator = NodeGenerator()

    def apply(bitStr: String): NodeId = {
        val (decVal, _) = bitStr.foldRight((BigInt(0), 0)) {
            case (c, (sum, index)) if c == '1' => (sum.setBit(index), index + 1)
            case (c, (sum, index)) if c == '0' => (sum, index + 1)
        }

        new NodeId(decVal, bitStr.length())
    }

    private def toUnsigned(bytes: Array[Byte]): BigInt = {
        def _toUnsigned(index: Int = 0, decVal: BigInt = BigInt(0)): BigInt = if (index == bytes.length) decVal else _toUnsigned(index + 1, (decVal << 8) + (bytes(index) & 0xff))
        _toUnsigned()
    }

    def apply(byteArray: Array[Byte]): NodeId = {
        val decVal = toUnsigned(byteArray)
        NodeId(decVal, byteArray.length * 8)
    }

    def apply(): NodeId = nodeGenerator.generateId

}
