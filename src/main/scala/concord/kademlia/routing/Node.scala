package concord.kademlia.routing

import akka.actor.ActorRef
import concord.identity.NodeId
import concord.util.Host


abstract class Node {
    val nodeId: NodeId
}

case class RemoteNode(host: Host, nodeId: NodeId) extends Node

case class ActorNode(ref: ActorRef, nodeId: NodeId) extends Node
