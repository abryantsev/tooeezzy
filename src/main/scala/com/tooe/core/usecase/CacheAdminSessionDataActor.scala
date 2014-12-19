package com.tooe.core.usecase

import com.tooe.core.util.ActorHelper
import akka.actor.Actor
import akka.pattern._
import com.tooe.core.domain.{AdminUserId, CompanyId, SessionToken}
import com.tooe.core.db.mongo.domain.CacheAdminSession
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.CacheAdminSessionDataService
import scala.concurrent.Future
import com.tooe.core.exceptions.ApplicationException
import spray.http.StatusCodes
import com.tooe.core.application.Actors


object CacheAdminSessionDataActor {
  final val Id = Actors.CacheAdminSessionData

  case class NewSession(session: CacheAdminSession)
  case class GetSession(token: SessionToken)
  case class Delete(token: SessionToken)
  case class AddCompany(userId: AdminUserId, companyId: CompanyId)
}

class CacheAdminSessionDataActor extends Actor with ActorHelper {

  lazy val service = BeanLookup[CacheAdminSessionDataService]

  import CacheAdminSessionDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  def receive = {
    case NewSession(session) => Future { service.save(session) } pipeTo sender

    case GetSession(token) => Future {
      service.find(token) getOrElse (throw sessionNotFoundException)
    } pipeTo sender

    case Delete(token) => Future {
      service.delete(token)
    }

    case AddCompany(userId, companyId) => Future(service.addCompany(userId, companyId))
  }

  def sessionNotFoundException =
    ApplicationException(message = "Either user has not authenticated or session has expired", statusCode = StatusCodes.Unauthorized)

}
