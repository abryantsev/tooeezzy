package com.tooe.core.infrastructure

import akka.actor._
import akka.cluster.ClusterEvent._

class ClusterListenerActor extends Actor with ActorLogging {
  
  def receive = {
    case state: CurrentClusterState =>
      println(">>>>>>>>>>>>>> Current members: {}", state.members.mkString(", "))
//      log.info(">>>>>>>>>>>>>> Current members: {}", state.members.mkString(", "))
      
    case MemberUp(member) =>
      println(">>>>>>>>>>>>>> Member is Up: {}", member.address)
//      log.info(">>>>>>>>>>>>>> Member is Up: {}", member.address)
      
    case MemberExited(member) =>
      println(">>>>>>>>>>>>>> Member exited: {}", member.address)
//      log.info(">>>>>>>>>>>>>> Member is Up: {}", member.address)

    case UnreachableMember(member) =>
      println(">>>>>>>>>>>>>> Member detected as unreachable: {}", member)
//      log.info(">>>>>>>>>>>>>> Member detected as unreachable: {}", member)
      
    case MemberRemoved(member, previousStatus) =>
      println(">>>>>>>>>>>>>> Member is Removed: {} after {}", member.address, previousStatus)
//      log.info(">>>>>>>>>>>>>> Member is Removed: {} after {}", member.address, previousStatus)
      
    case ev: ClusterDomainEvent =>  // ignore
//      println(">>>>>>>>>>>>>> ClusterDomainEvent... "+ev.toString()) 
  }
}