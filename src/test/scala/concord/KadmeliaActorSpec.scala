package concord

import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterEach, FunSpecLike, Matchers}


class KadmeliaActorSpec extends FunSpecLike with Matchers with BeforeAndAfterEach {

    implicit val system = ActorSystem()

    describe("kademlia actor") {

    }

}
