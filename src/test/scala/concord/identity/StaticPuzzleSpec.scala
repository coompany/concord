package concord.identity

import org.scalatest.{FunSpecLike, Matchers}


class StaticPuzzleSpec extends FunSpecLike with Matchers {

    describe("StaticPuzzle") {

        it("should validate a valid solution") {
            val sp = StaticPuzzle("SHA-1", 3)
            val nodeId = NodeId("0000001101100011010100010010011101110100100100010100010100011100000000010100001100101000101110110101000100101110100011100010111010100010111101001001010100010111")
            sp.verifySolution(nodeId) should be(true)
        }

        it("should generate a valid NodeId") {
            val sp = StaticPuzzle("SHA-1", 3)
            val (_, nodeId) = sp.findPair("RSA")
            sp.verifySolution(nodeId) should be(true)
        }

    }

}
