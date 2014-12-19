package com.tooe.core.usecase

import com.tooe.core.application.{AppActors, Actors}
import com.tooe.api.service.{RouteContext, OffsetLimit, SuccessfulResponse}
import akka.actor.Actor
import akka.pattern._
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.domain._
import com.tooe.core.usecase.star_subscription.StarSubscriptionDataActor
import com.tooe.core.db.mongo.domain._
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.api._
import com.tooe.core.usecase.star_category.StarsCategoriesDataActor
import com.tooe.core.db.mongo.domain.StarSubscription
import com.tooe.core.usecase.star_subscription.StarSubscriptionDataActor.CountStarSubscribers
import com.tooe.core.domain.UserId
import com.tooe.core.domain.MediaUrl
import com.tooe.core.domain.StarCategoryId
import com.tooe.core.usecase.star_subscription.StarSubscriptionDataActor.FindStarSubscribers

object StarSubscriptionActor {
  final val Id = Actors.StarSubscription

  case class Subscribe(userId: UserId, starId: UserId)
  case class UnSubscribe(userId: UserId, starId: UserId)
  case class GetStarSubscriptionUsers(userId: UserId, offsetLimit: OffsetLimit)
  case class GetUserOwnStarSubscription(userId: UserId, offsetLimit: OffsetLimit)
}

class StarSubscriptionActor extends Actor with AppActors with DefaultTimeout {

  import StarSubscriptionActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val starSubscriptionDataActor = lookup(StarSubscriptionDataActor.Id)
  lazy val userReadActor = lookup(UserReadActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val starsCategoriesDataActor = lookup(StarsCategoriesDataActor.Id)

  def receive = {

    case Subscribe(userId, starId) => {
      starSubscriptionDataActor ! StarSubscriptionDataActor.AddSubscription(userId, starId)
      updateStatisticActor ! UpdateStatisticActor.ChangeUserStarSubscriptionsCounter(userId, 1)
      updateStatisticActor ! UpdateStatisticActor.ChangeStarSubscriptionsCounter(starId, 1)

      sender ! SuccessfulResponse
    }

    case UnSubscribe(userId, starId) => {
      starSubscriptionDataActor ! StarSubscriptionDataActor.Unsubscribe(userId, starId)
      updateStatisticActor ! UpdateStatisticActor.ChangeUserStarSubscriptionsCounter(userId, -1)
      updateStatisticActor ! UpdateStatisticActor.ChangeStarSubscriptionsCounter(starId, -1)

      sender ! SuccessfulResponse
    }

    case GetStarSubscriptionUsers(userId, offsetLimit) =>
      (for {
        (subscription, count) <- (starSubscriptionDataActor ? FindStarSubscribers(userId, offsetLimit))
                                .zip(starSubscriptionDataActor ? CountStarSubscribers(userId)).mapTo[(Seq[StarSubscription], Long)]
        users <- (userReadActor? UserReadActor.GetSubscriberStarsItems(subscription.map(_.userId))).mapTo[Seq[SubscriberStarsItem]]
      } yield {
        StarSubscriptionResponse(count, users)
      }) pipeTo sender

    case GetUserOwnStarSubscription(userId, offsetLimit) =>
      (for {
        (userSubscriptions, count) <- (starSubscriptionDataActor ? StarSubscriptionDataActor.GetStarsByUserSubscription(userId, offsetLimit))
          .zip(starSubscriptionDataActor ? StarSubscriptionDataActor.GetStarsByUserSubscriptionCount(userId)).mapTo[(Seq[StarSubscription], Long)]
        stars <- (userReadActor ? UserReadActor.GetUserStarSubscriptionsItems(userSubscriptions.map(_.starId))).mapTo[Seq[StarSubscriptionItem]]
      } yield UserSubscriptionResponse(count, stars)) pipeTo sender
  }
}

case class UserStarsResponse(users: Seq[StarItemResponse]) extends SuccessfulResponse

object UserStarsResponse {

  def apply(stars: Seq[StarItem], categories: Map[StarCategoryId, Option[String]]): UserStarsResponse = {
    UserStarsResponse(stars.map { star =>
      StarItemResponse(
        id = star.id,
        name = star.name,
        lastName =  star.lastName,
        media = star.media,
        categories = star.categories.map(c => StarCategoryResponse(c, categories(c).getOrElse("")))
      )
    })
  }

}

case class StarSubscriptionRequest(@JsonProperty("starid") starId: UserId) extends UnmarshallerEntity

case class StarSubscriptionResponse(@JsonProperty("userscount") count: Long, users: Seq[SubscriberStarsItem]) extends SuccessfulResponse

case class StarItemResponse(
  @JsonProp("id") id: UserId,
  @JsonProp("name") name: String,
  @JsonProp("lastname") lastName: Option[String],
  @JsonProp("media") media: MediaUrl,
  categories: Seq[StarCategoryResponse]
)

case class StarCategoryResponse(@JsonProp("categoryid") id: StarCategoryId, name: String)

case class UserSubscriptionResponse(@JsonProp("starscount") count: Long, stars: Seq[StarSubscriptionItem]) extends SuccessfulResponse