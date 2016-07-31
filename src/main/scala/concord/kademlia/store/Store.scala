package concord.kademlia.store

import concord.identity.NodeId


trait Store[V] {

    def get(key: NodeId): Option[V]
    def set(key: NodeId, value: V)

}
