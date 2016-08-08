package concord.identity

import org.scalatest.{FunSpecLike, Matchers}


class PuzzleSpec extends FunSpecLike with Matchers {

    describe("Puzzle") {

        it("should validate a valid static solution") {
            val puzzle = Puzzle("SHA-1", 3)
            val nodeId = NodeId("0000001101100011010100010010011101110100100100010100010100011100000000010100001100101000101110110101000100101110100011100010111010100010111101001001010100010111")
            puzzle.verifyStatic(nodeId) should be(true)
        }

        it("should generate a valid static NodeId") {
            val puzzle = Puzzle("SHA-1", 3)
            val pair = puzzle.findPair("RSA")
            val nodeId = NodeId(puzzle.digest.digest(pair.getPublic.getEncoded))
            puzzle.verifyStatic(nodeId) should be(true)
        }

        it("should validate a valid nonce") {
            val puzzle = Puzzle("SHA-1", 3)
            val nodeId = NodeId("0000001101100011010100010010011101110100100100010100010100011100000000010100001100101000101110110101000100101110100011100010111010100010111101001001010100010111")
            puzzle.verifyDynamic(nodeId, 3)
        }

    }

}
