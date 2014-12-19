package com.tooe.core.usecase.present

import akka.actor.Actor
import akka.pattern.pipe
import com.tooe.core.util.ActorHelper
import com.tooe.core.application.AppActors
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.PresentDataService
import com.tooe.core.application.Actors
import scala.concurrent.Future
import com.tooe.core.domain._
import com.tooe.api.service.OffsetLimit
import com.tooe.core.usecase.OptionWrapper
import com.tooe.core.usecase.job.urls_check.ChangeUrlType
import java.math.BigInteger
import com.tooe.api.service.GetPresentParameters
import com.tooe.core.service.PresentAdminSearchParams
import com.tooe.core.db.mongo.domain.Present
import com.tooe.api.service.SentPresentsRequest

object PresentDataActor {
  val Id = Actors.PresentData

  case class Save(entity: Present)
  case class Find(ids: Set[PresentId])
  case class MarkAsRemoved(presentId: PresentId, userId: UserId)
  case class GetUserPresents(userId: UserId, parameters: GetPresentParameters, offsetLimit: OffsetLimit)
  case class GetUserPresentsCount(userId: UserId, parameters: GetPresentParameters)
  case class FindPresent(id: PresentId)
  case class GetPresent(id: PresentId)
  case class ActivatePresent(id: PresentId)
  case class GetUserPresentByCode(code: PresentCode)
  case class CommentPresent(presentId: PresentId, comment: String)
  case class PresentsAdminSearch(params: PresentAdminSearchParams)
  case class PresentsAdminSearchCount(params: PresentAdminSearchParams)
  case class GetUserSentPresents(userId: UserId, request: SentPresentsRequest, offsetLimit: OffsetLimit)
  case class GetUserSentPresentsCount(userId: UserId, request: SentPresentsRequest)
  case class FindUserPresentsByProduct(userId: UserId, productId: ProductId)
  case class FindByOrderIds(orderIds: Seq[BigInteger])
  case class AssignUserPresents(phone: Option[PhoneShort], email: Option[String], userId: UserId)
}

class PresentDataActor extends Actor with ActorHelper with AppActors {

  lazy val service = BeanLookup[PresentDataService]

  import PresentDataActor._
  import context.dispatcher

  def receive = {
    case Save(present) => Future(service.save(present))
    case Find(ids) => Future(service.find(ids)) pipeTo sender
    case GetUserPresents(userId, parameters, offsetLimit) => Future(service.getUserPresents(userId, parameters, offsetLimit)) pipeTo sender
    case GetUserPresentsCount(userId, parameters) => Future(service.userPresentsCount(userId, parameters)) pipeTo sender
    case FindPresent(id) => Future {
      service.find(id)
    } pipeTo sender
    case ActivatePresent(presentId) => Future(service.activatePresent(presentId)) pipeTo sender
    case GetUserPresentByCode(code) => Future(service.findByCode(code).getOrNotFound(code, s"""Present with code "${code}" not found""")) pipeTo sender
    case CommentPresent(presentId, comment) => Future(service.commentPresent(presentId, comment))
    case GetPresent(id) => Future(service.find(id).getOrNotFound(id, s"""Present with code "${id}" not found""")) pipeTo sender
    case PresentsAdminSearch(params) => Future(service.findByAdminCriteria(params)).pipeTo(sender)
    case PresentsAdminSearchCount(params) => Future(service.countByAdminCriteria(params)).pipeTo(sender)
    case MarkAsRemoved(presentId, userId) => Future(service.markAsRemoved(presentId, userId)).pipeTo(sender)

    case msg: ChangeUrlType.ChangeTypeToS3 => Future { service.updateMediaStorageToS3(PresentId(msg.url.entityId), msg.newMediaId) }

    case msg: ChangeUrlType.ChangeTypeToCDN => Future { service.updateMediaStorageToCDN(PresentId(msg.url.entityId)) }

    case GetUserSentPresents(userId, request, offsetLimit) => Future { service.getUserSentPresents(userId, request, offsetLimit) } pipeTo sender

    case GetUserSentPresentsCount(userId, request) => Future { service.getUserSentPresentsCount(userId, request) } pipeTo sender

    case FindUserPresentsByProduct(userId, productId) => Future(service.findUserPresents(userId, productId)) pipeTo sender

    case FindByOrderIds(orderIds) => Future { service.findByOrderIds(orderIds) } pipeTo sender

    case AssignUserPresents(phone, email, userId) => Future(service.assignUserPresents(phone, email)(userId))
  }
}