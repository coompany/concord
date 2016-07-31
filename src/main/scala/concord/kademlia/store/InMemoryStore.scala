package concord.kademlia.store
import concord.identity.NodeId

import scala.collection.mutable


trait InMemoryStore[V] extends Store[V] {

    protected[store] val map = mutable.Map[NodeId, V]()

    override def get(key: NodeId): Option[V] = map.get(key)

    override def set(key: NodeId, value: V): Unit = map.update(key, value)
}
