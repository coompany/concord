package concord

import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import concord.kademlia.KademliaActor
import org.scalatest.{BeforeAndAfterEach, FunSpecLike, Matchers}


class KadmeliaActorSpec extends FunSpecLike with Matchers with BeforeAndAfterEach {

    implicit val system = ActorSystem()

    describe("kademlia actor") {
        val actor = TestActorRef[KademliaActor](new KademliaActor())
        it("should exist") {
            actor.underlyingActor should exist
        }
    }

}
