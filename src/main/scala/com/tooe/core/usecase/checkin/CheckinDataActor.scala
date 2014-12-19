package com.tooe.core.usecase.checkin

import com.tooe.core.application.{AppActors, Actors}
import akka.actor.Actor
import com.tooe.core.util.ActorHelper
import com.tooe.core.exceptions.NotFoundException
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.{SearchNearParams, CheckinDataService}
import com.tooe.core.db.mongo.domain.Checkin
import com.tooe.core.domain.{LocationId, UserId}
import concurrent.Future
import akka.pattern.pipe
import com.tooe.api.service.OffsetLimit
import com.tooe.core.usecase.job.urls_check.ChangeUrlType.ChangeTypeToCDN

object CheckinDataActor {
  final val Id = Actors.CheckinData

  case class SaveCheckin(checkin: Checkin)
  case class RemoveUserCheckins(userId: UserId)
  case class FindCheckinByUserId(userId: UserId)
  case class GetCheckinByUserId(userId: UserId)
  case class FindCheckins(locationIds: Seq[LocationId])

  case class SearchNearOrderedByDistance(request: SearchNearParams)
  case class SearchNearCount(request: SearchNearParams)
  case class SearchNearOrderedByName(request: SearchNearParams)
  
  case class GetOnlyCheckedInUsers(userIds: Seq[UserId], locationId: LocationId)
  case class FindAllCheckedInUsers(locationId: LocationId, currentUserId: UserId, offsetLimit: OffsetLimit)
  case class CountAllCheckedInUsers(locationId: LocationId, currentUserId: UserId)
  case class CountCheckedInFriends(locationId: LocationId, currentUserId: UserId)
  case class FindCheckedInFriends(locationId: LocationId, currentUserId: UserId, offsetLimit: OffsetLimit)
}

class CheckinDataActor extends Actor with ActorHelper with AppActors{

  lazy val service = BeanLookup[CheckinDataService]

  import CheckinDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  val CheckinSearchRadius = settings.GeoSearch.CheckinSearch.RadiusDefault

  def receive = {
    case GetOnlyCheckedInUsers(userIds, locationId) =>
      Future{ service.findCheckedInUsers(userIds, locationId)} pipeTo sender

    case FindAllCheckedInUsers(locationId, currentUserId, offsetLimit) =>
      Future{ service.findCheckedInUsers(locationId, currentUserId, offsetLimit)} pipeTo sender

    case CountAllCheckedInUsers(locationId, currentUserId) =>
      Future{ service.countCheckedInUsers(locationId, currentUserId)} pipeTo sender

    case CountCheckedInFriends(locationId, currentUserId) =>
      Future{ service.countCheckedInFriends(locationId, currentUserId)} pipeTo sender

    case FindCheckedInFriends(locationId, currentUserId, offsetLimit) =>
      Future{ service.findCheckedInFriends(locationId, currentUserId, offsetLimit)} pipeTo sender

    case FindCheckins(locationIds) =>
      Future{ service.findCheckins(locationIds)} pipeTo sender

    case SaveCheckin(checkin) => Future(service.save(checkin)) pipeTo sender

    case FindCheckinByUserId(userId) => Future(service.findUsersCheckin(userId)) pipeTo sender

    case GetCheckinByUserId(userId) => Future{
      service.findUsersCheckin(userId) getOrElse( throw NotFoundException("Checkin not found"))
    } pipeTo sender

    case SearchNearOrderedByDistance(request) =>
      Future(service.searchNearOrderedByDistance(request, CheckinSearchRadius)) pipeTo sender

    case SearchNearCount(request) =>
      Future(service.searchNearCount(request, CheckinSearchRadius)) pipeTo sender

    case SearchNearOrderedByName(request) =>
      Future(service.searchNearOrderedByName(request, CheckinSearchRadius)) pipeTo sender

    case RemoveUserCheckins(userId) => service.removeUserCheckins(userId)

    case ChangeTypeToCDN(url) => Future { service.changeUrlTypeToCdn(url) }
  }
}