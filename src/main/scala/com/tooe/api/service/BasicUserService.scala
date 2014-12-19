package com.tooe.api.service

import scala.util.{ Success, Failure }
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.db.mongo.{SaveBasic, MongoDataActor, GetBasic, ListBasicUsers}
import com.tooe.core.db.mongo.domain.BasicUser
import akka.actor.{Props, ActorSystem, actorRef2Scala}
import akka.pattern.ask
import spray.httpx.marshalling.MetaMarshallers
import spray.routing.Directive.pimpApply
import spray.routing.{PathMatcher, Directives}
import com.tooe.api.MetaData.Rest._
import com.tooe.api.marshalling.Marshalling

class BasicUserService(implicit val actorSystem: ActorSystem)
  extends Directives with Marshalling with MetaMarshallers with DefaultTimeout {

  import scala.concurrent.ExecutionContext.Implicits.global
  import BasicUserService._
  //val basicUserActor = actorSystem.actorFor("/user/application/mongodata")
  //implicit val timeout = Timeout(1000)
  val basicUserActor = actorSystem.actorOf(Props[MongoDataActor])

  val route =
    pathPrefix(MONGO_PATH){
      path(PathMatcher("testget")) {
        get {
          complete {
            basicUserActor ! GetBasic()

            "mongodata ! GetBasic() -> fire and forget"
          }
        }
      } ~
      path(PathMatcher("savebasic")) {
        get {
          complete {
            basicUserActor ! SaveBasic()

            "savebasic compleated"
          }
        }
      } ~
      path(PathMatcher("getbasicusers")) {
        get{ ctx =>
            basicUserActor.ask(ListBasicUsers()).mapTo[List[BasicUser]]
            .onComplete {
            case Success(users) => ctx.complete {
              "Users.size : " + users.size + '\n' +
                "Users : " + users + '\n'
            }
            case Failure(ex) => ctx.complete(500, "Couldn't get server stats due to " + ex.getMessage)
          }
        }
      }
    }

}

object BasicUserService {
  private[BasicUserService] val MONGO_PATH = PathMatcher(Mongo)
  private[BasicUserService] val SAVE_USER_PATH = PathMatcher("saveuser")
}