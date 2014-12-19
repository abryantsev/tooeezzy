package com.tooe.core.usecase

import com.tooe.api.service.{SuccessfulResponse, OffsetLimit, ExecutionContextProvider}
import com.tooe.core.application.Actors
import com.tooe.core.domain._
import com.tooe.api._
import com.tooe.core.usecase.promotion.PromotionVisitorDataActor
import scala.concurrent.Future
import com.tooe.api.validation.{ValidationContext, Validatable}
import com.tooe.core.domain.PromotionId
import com.tooe.core.db.mongo.domain.PromotionVisitor
import com.tooe.core.domain.UserId

object PromotionVisitorReadActor {
  final val Id = Actors.PromotionVisitorRead

  case class PromotionVisitorsShortByPromotions(promotionIds: Set[PromotionId], userId: UserId)
  case class GetVisitorUserInfoShortItems(promotionId: PromotionId, offsetLimit: OffsetLimit)
  case class GetPromotionVisitors
  (
    userId: UserId,
    promotionId: PromotionId,
    fieldsOpt: Option[Set[PromotionVisitorsField]],
    offsetLimit: OffsetLimit
    ) extends Validatable
  {
    import PromotionVisitorsField._

    def defaultFields = Set[PromotionVisitorsField](Users, UsersCount, FriendsCount)

    def fields = fieldsOpt getOrElse defaultFields

    def validate(ctx: ValidationContext) =
      if (offsetLimit.offset > 0 & includeOnlyCounters) {
        ctx.fail("Incorrect parameter combination: offset > 0 and only counters requested " + fields.mkString("[", ", ", "]"))
      }

    def includeOnlyCounters = !includeUsers
    def includeUsers = fields contains Users
    def includeUsersCount = fields contains UsersCount
    def includeFriendsCount = fields contains FriendsCount
  }
}

trait PromotionVisitorReadComponent { self: AppActor with ExecutionContextProvider =>
  lazy val promotionVisitorReadActor = lookup(PromotionVisitorReadActor.Id)

  import PromotionVisitorReadActor._

  def getPromotionVisitorsShortMap(promotionIds: Set[PromotionId], userId: UserId): Future[Map[PromotionId, PromotionVisitorCounters]] =
    (promotionVisitorReadActor ? PromotionVisitorsShortByPromotions(promotionIds, userId)).mapTo[Map[PromotionId, PromotionVisitorCounters]]

  def getVisitorUserInfoShortItems(promotionId: PromotionId, offsetLimit: OffsetLimit): Future[Seq[UserInfoShort]] =
    (promotionVisitorReadActor ? GetVisitorUserInfoShortItems(promotionId, offsetLimit)).mapTo[Seq[UserInfoShort]]
}

class PromotionVisitorReadActor extends AppActor with ExecutionContextProvider with FriendReadComponent {

  lazy val promotionVisitorDataActor = lookup(PromotionVisitorDataActor.Id)
  lazy val userReadActor = lookup(UserReadActor.Id)

  import PromotionVisitorReadActor._

  def receive = {
    case PromotionVisitorsShortByPromotions(promotionIds, userId) =>
      def promotionVisitorsShortMap(visitors: Seq[PromotionVisitor], userFriendIds: Set[UserId]): Map[PromotionId, PromotionVisitorCounters] =
        visitors.groupBy(_.promotion).map {
          case (pid, vs) => pid -> PromotionVisitorCounters(userFriendIds)(pid, vs)
        }.toMap

      val future = for {
        visitors <- findByPromotionIds(promotionIds)
        userFriendIds <- getUserFriends(userId)
      } yield promotionVisitorsShortMap(visitors, userFriendIds) withDefaultValue PromotionVisitorCounters.zero

      future pipeTo sender

    case GetVisitorUserInfoShortItems(promotionId, offsetLimit: OffsetLimit) =>
      getVisitorUserInfoShortItems(promotionId,"",  offsetLimit) pipeTo sender

    case request: GetPromotionVisitors =>

      import request._

      def countUsersAndFriends: Future[(Option[Long], Option[Long])] =
        for {
          visitorUserIds <- findVisitorUserIds(promotionId)
          userFriendIds <- getUserFriends(userId)
          visitorFriends = userFriendIds & visitorUserIds
        } yield (Some(visitorUserIds.size), Some(visitorFriends.size))

      def countUsersAndFriendsOptFtr: Future[(Option[Long], Option[Long])] =
        if (includeUsersCount && includeFriendsCount) countUsersAndFriends
        else if (includeFriendsCount) countUsersAndFriends map { case (_, friendsCount) => (None, friendsCount) }
        else if (includeUsersCount) countPromotionVisitors(promotionId) map (usersCount => (Some(usersCount), None))
        else Future successful (None, None)

      val future = for {
        (usersCountOpt, friendsOpt) <- countUsersAndFriendsOptFtr
        userItems <- getVisitorUserInfoShortItems(promotionId,"", offsetLimit)
      } yield UsersGoingToPromotion(usersCount = usersCountOpt, friendsCount = friendsOpt, users = userItems)
      future pipeTo sender
  }

  def countPromotionVisitors(promotionId: PromotionId): Future[Long] =
    (promotionVisitorDataActor ? PromotionVisitorDataActor.CountPromotionVisitors(promotionId)).mapTo[Long]

  def findVisitorUserIds(promotionId: PromotionId): Future[Set[UserId]] =
    (promotionVisitorDataActor ? PromotionVisitorDataActor.FindVisitorUserIds(promotionId)).mapTo[Set[UserId]]

  def findByPromotionIds(promotionIds: Set[PromotionId]): Future[Seq[PromotionVisitor]] =
    (promotionVisitorDataActor ? PromotionVisitorDataActor.FindByPromotions(promotionIds)).mapTo[Seq[PromotionVisitor]]

  def getVisitorUserInfoShortItems(id: PromotionId, imageSize: String, offsetLimit: OffsetLimit): Future[Seq[UserInfoShort]] =
    getPromotionVisitorUserIds(id, offsetLimit) flatMap getUserInfoShortItems(imageSize)

  def getUserInfoShortItems(imageSize: String)(ids: Seq[UserId]): Future[Seq[UserInfoShort]] =
    (userReadActor ? UserReadActor.GetUserInfoShortItems(ids, imageSize)).mapTo[Seq[UserInfoShort]]

  def getPromotionVisitorUserIds(id: PromotionId, offsetLimit: OffsetLimit): Future[Seq[UserId]] =
    getPromotionVisitors(id, offsetLimit) map (_ map (_.visitor))

  def getPromotionVisitors(promotionId: PromotionId, offsetLimit: OffsetLimit): Future[Seq[PromotionVisitor]] =
    (promotionVisitorDataActor ? PromotionVisitorDataActor.GetPromotionVisitors(promotionId, offsetLimit)).mapTo[Seq[PromotionVisitor]]
}

case class ShowPromotionVisitorsResponse(visitors: PromotionVisitors) extends SuccessfulResponse

object ShowPromotionVisitorsResponse {
  def apply(users: Seq[UserInfoShort]): ShowPromotionVisitorsResponse = ShowPromotionVisitorsResponse(PromotionVisitorsFull(users))
}

case class UsersGoingToPromotion
(
  @JsonProp("userscount") usersCount: Option[Long],
  @JsonProp("friendscount") friendsCount: Option[Long],
  @JsonProp("users") users: Seq[UserInfoShort]
  ) extends SuccessfulResponse

trait PromotionVisitors

case class PromotionVisitorCounters
(
  @JsonProp("users_count") usersCount: Int,
  @JsonProp("friends_count") friendsCount: Int
  ) extends PromotionVisitors

object PromotionVisitorCounters {
  def apply(fiendUserIds: Set[UserId])(promotionId: PromotionId, visitors: Seq[PromotionVisitor]): PromotionVisitorCounters = {
    val visitorUserIds = visitors.map(_.visitor).toSet
    PromotionVisitorCounters(
      usersCount = visitorUserIds.size,
      friendsCount = (visitorUserIds & fiendUserIds).size
    )
  }

  def zero: PromotionVisitorCounters = PromotionVisitorCounters(0, 0)
}

case class PromotionVisitorsFull
(
  @JsonProp("users_count") usersCount: Int,
  @JsonProp("friends_count") friendsCount: Option[Int],
  users: Seq[UserInfoShort],
  friends: Option[Seq[UserInfoShort]]
  ) extends PromotionVisitors

object PromotionVisitorsFull {
  def apply(userItems: Seq[UserInfoShort]): PromotionVisitorsFull = PromotionVisitorsFull(
    userItems.size,
    None, // TODO friends count
    userItems,
    None
  )
}