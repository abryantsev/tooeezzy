package com.tooe.api.service

import akka.actor.ActorSystem
import com.tooe.api.boot.TestService
import spray.routing.PathMatcher
import com.tooe.core.usecase.{CreateLinkRequest, CreateLinkMediaServerActor}
import com.tooe.core.usecase.CreateLinkMediaServerActor.CreateLink

//TODO: service for test create link in media server
class CreateLinkMediaServerService (implicit val system: ActorSystem) extends SprayServiceBaseClass2 with TestService {

  import scala.concurrent.ExecutionContext.Implicits.global
  import CreateLinkMediaServerService._

  lazy val createLinkMediaServerActor = lookup(CreateLinkMediaServerActor.Id)

  val route = (mainPrefix & path(Root)) { routeContext: RouteContext =>
    post {
      entity(as[CreateLinkRequest]) { request: CreateLinkRequest =>
        authenticateBySession { userSession: UserSession =>
          complete {
            createLinkMediaServerActor ! CreateLink(request.target, request.source, request.folder)
            "created"
          }
        }
      }
    }
  }

}

object CreateLinkMediaServerService {
  val Root = PathMatcher("createlink")
}