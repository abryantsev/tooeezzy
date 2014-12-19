package com.tooe.core.usecase.star_subscription

import com.tooe.core.application.{AppActors, Actors}
import akka.actor.Actor
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.domain.UserId
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.StarSubscriptionDataService
import scala.concurrent.Future
import akka.pattern._
import com.tooe.core.db.mongo.domain.StarSubscription
import com.tooe.api.service.OffsetLimit

object StarSubscriptionDataActor {
  final val Id = Actors.StarSubscriptionData

  case class AddSubscription(userId: UserId, starId: UserId)
  case class Unsubscribe(userId: UserId, starId: UserId)
  case class FindUsersStarSubscriptions(userId: UserId)
  case class ExistSubscribe(starId: UserId, userId: UserId)
  case class FindStarSubscribers(starId: UserId, offsetLimit: OffsetLimit)
  case class CountStarSubscribers(starId: UserId)
  case class GetStarsByUserSubscription(userId: UserId, offsetLimit: OffsetLimit)
  case class GetStarsByUserSubscriptionCount(userId: UserId)
}

class StarSubscriptionDataActor extends Actor with AppActors with DefaultTimeout {

  import StarSubscriptionDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  val service = BeanLookup[StarSubscriptionDataService]

  def receive = {
    case AddSubscription(userId, starId) =>
      Future {
        val starSubscription = StarSubscription(userId = userId, starId = starId)
        service.save(starSubscription)
      } pipeTo sender

    case Unsubscribe(userId, starId) => service.removeSubscription(userId, starId)

    case FindUsersStarSubscriptions(userId) => Future(service.findByUser(userId)) pipeTo sender

    case ExistSubscribe(starId, userId) => Future(service.existSubscribe(starId, userId)) pipeTo sender

    case FindStarSubscribers(starId, offsetLimit) => Future { service.findStarSubscribers(starId, offsetLimit) } pipeTo sender

    case CountStarSubscribers(starId: UserId) => Future { service.countStarSubscribers(starId) } pipeTo sender

    case GetStarsByUserSubscription(userId, offsetLimit) => Future { service.getStarsByUserSubscription(userId, offsetLimit) } pipeTo sender

    case GetStarsByUserSubscriptionCount(userId) => Future { service.getStarsByUserSubscriptionCount(userId) } pipeTo sender

  }
}