package com.tooe.api.service

import com.tooe.api.boot.TestService
import com.tooe.core.domain.UserId
import akka.actor.ActorSystem
import akka.pattern._
import spray.routing.PathMatcher
import com.tooe.core.db.graph.msg.GraphGetFriends
import akka.util.Timeout
import com.tooe.core.db.graph.msg.GraphFriends

class TestServiceGraph (implicit val system: ActorSystem) extends SprayServiceBaseClass2 with TestService {

  import scala.concurrent.ExecutionContext.Implicits.global
  import TestService1._

  lazy val getFriendsGraphActor = lookup(Symbol("graphGetFriends"))

  val route = (mainPrefix & path(Root)) { routeContext: RouteContext =>
      get {
          parameter('userId )  { (userId: String) =>
            val extractedLocalValue = UserId()
            complete(
               getFriendsGraphActor.ask(new GraphGetFriends(extractedLocalValue))(Timeout(10000)).mapTo[GraphFriends]
            )
          }
      }
  }

}

object TestServiceGraph {
  val Root = PathMatcher("gtest1")
}
