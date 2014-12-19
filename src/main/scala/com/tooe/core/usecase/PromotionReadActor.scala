package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.fasterxml.jackson.annotation.JsonProperty
import promotion.{PromotionVisitorDataActor, PromotionSearchSortType, PromotionDataActor}
import java.util.Date
import com.tooe.api.service.{ExecutionContextProvider, OffsetLimit, SuccessfulResponse, RouteContext}
import com.tooe.core.util.{Lang, DateHelper}
import org.bson.types.ObjectId
import com.tooe.core.domain._
import concurrent.Future
import com.tooe.core.db.mongo.domain.PromotionVisitor
import com.tooe.core.db.mongo.domain.Promotion
import com.tooe.core.domain.PromotionId
import com.tooe.core.usecase.LocationReadActor.GetPromotionLocationFullItem
import com.tooe.core.domain.LocationId
import com.tooe.core.domain.UserId
import com.tooe.core.domain.MediaUrl
import com.tooe.core.db.mongo.domain.promotion.Dates
import com.tooe.api.{JsonSkip, JsonProp}
import com.tooe.api.validation.{ValidationContext, Validatable}

object PromotionReadActor {
  final val Id = Actors.PromotionRead

  case class GetPromotion(request: GetPromotionRequest, userId: UserId, ctx: RouteContext)
  case class SearchPromotions(request: SearchPromotionsRequest, userId: UserId, lang: Lang)
  case class FindUserEventPromotions(ids: Set[PromotionId], lang: Lang)
}

class PromotionReadActor extends AppActor with ExecutionContextProvider
  with PromotionVisitorReadComponent
{
  lazy val promotionDataActor = lookup(PromotionDataActor.Id)
  lazy val promotionVisitorDataActor = lookup(PromotionVisitorDataActor.Id)
  lazy val locationReadActor = lookup(LocationReadActor.Id)

  import PromotionReadActor._

  def receive = {

    case GetPromotion(request, userId, ctx) =>
      val lang = ctx.lang

      val future = for {
        promotion <- promotionDataActor.ask(PromotionDataActor.GetPromotion(request.promotionId)).mapTo[Promotion]
        fullLocationItem <- getLocationFullItem(lang)(promotion.location.location)
        userItemsOpt <- if (request.viewType == ViewType.Short) Future successful None
                        else getVisitorUserInfoShortItems(request.promotionId, OffsetLimit(0, 6)) map { items => Some(items) }
        selfVisit <- (promotionVisitorDataActor ? PromotionVisitorDataActor.Find(promotion.id, userId)).mapTo[Option[PromotionVisitor]]
      } yield GetPromotionResponse(promotion, fullLocationItem, userItemsOpt, selfVisit.map(_.status == PromotionStatus.Confirmed))(lang)

      future pipeTo sender

    case SearchPromotions(request, userId, lang) =>

      val future = for {
        promotions <- searchPromotions(request, lang)
        promotionIds = promotions.map(_.id).toSet
        promotionVisitorsShortMap <- if (request.includeVisitorCounts) getPromotionVisitorsShortMap(promotionIds, userId) map (m => Some(m))
                                     else Future successful None
        countOpt <- if (request.includeCount) searchPromotionsCount(request, lang) map Some.apply else Future successful None
      } yield SearchPromotionsResponse(promotionVisitorsShortMap, lang, count = countOpt)(promotions)

      future pipeTo sender

    case FindUserEventPromotions(ids, lang) => findPromotions(ids) map (_ map UserEventPromotion(lang)) pipeTo sender
  }

  def searchPromotions(request: SearchPromotionsRequest, lang: Lang): Future[Seq[Promotion]] =
    (promotionDataActor ? PromotionDataActor.SearchPromotions(request, lang)).mapTo[Seq[Promotion]]

  def searchPromotionsCount(request: SearchPromotionsRequest, lang: Lang): Future[Long] =
    (promotionDataActor ? PromotionDataActor.SearchPromotionsCount(request, lang)).mapTo[Long]

  def findPromotions(ids: Set[PromotionId]) = (promotionDataActor ? PromotionDataActor.FindPromotions(ids)).mapTo[Seq[Promotion]]

  def getLocationFullItem(lang: Lang)(id: LocationId) =
    (locationReadActor ? GetPromotionLocationFullItem(id, lang)).mapTo[PromotionFullLocation]
}

case class GetPromotionRequest(promotionId: PromotionId, view: Option[ViewType]) {
  def viewType: ViewType = view getOrElse ViewType.None
}

case class SearchPromotionsRequest
(
  @JsonProp("region") regionId: RegionId,
  @JsonProp("category") categoryId: Option[String],
  name: Option[String],
  date: Option[Date],
  sort: Option[PromotionSearchSortType],
  @JsonProp("isfavorite") isFavorite: Option[Boolean],
  @JsonProp("entities") fieldsOpt: Option[Set[PromotionFields]],
  offsetLimit: OffsetLimit
  ) extends Validatable
{
  import PromotionFields._

  val defaultFields = Set[PromotionFields](Promotions, PromotionsCount)

  def fields = fieldsOpt getOrElse defaultFields

  def validate(ctx: ValidationContext) = {
    ctx.checkOnlyOneAllowed(Set[PromotionFields](PromotionsShort, Promotions), fields)
    if ((fields == Set(PromotionsCount)) && offsetLimit.offset > 0) {
      ctx.fail(s"Not allowed to have offset > 0 and entities=${PromotionsCount.id}, because it doesn't return count for offset > 0")
    }
  }

  def includeVisitorCounts = fields contains PromotionsCount

  def includeCount = (fields contains PromotionsCount) && offsetLimit.offset == 0
}

case class SearchPromotionsResponse
(
  @JsonProp("promotionscount") promotionsCount: Option[Long],
  promotions: Seq[PromotionDataItem]
) extends SuccessfulResponse

object SearchPromotionsResponse {
  def apply(promotionVisitorsShortMap: Option[Map[PromotionId, PromotionVisitorCounters]], lang: Lang, count: Option[Long])(promotions: Seq[Promotion]): SearchPromotionsResponse =
    SearchPromotionsResponse(count, promotions map PromotionDataItem(promotionVisitorsShortMap, lang))
}

case class GetPromotionResponse(promotion: PromotionDataItem) extends SuccessfulResponse

object GetPromotionResponse {
  def apply(p: Promotion, l: PromotionFullLocation, usersOpt: Option[Seq[UserInfoShort]], selfVisitor: Option[Boolean])(lang: Lang): GetPromotionResponse = GetPromotionResponse(
    PromotionDataItem(
      p.id.id,
      p.name(lang),
      p.description(lang),
      PromotionDates(p.dates),
      p.price.map(_.getWithDefaultKey(lang)),
      l,
      p.media.head,
      usersOpt.map(users =>
        PromotionVisitorsFull( //TODO request it from PromotionVisitorReadActor
          p.visitorsCount,
          None,
          users,
          None
        )).orElse(Some(PromotionVisitorCounters(p.visitorsCount, 0))),
      selfVisitor
    )
  )
}

case class PromotionDataItem(
                          id: ObjectId,
                          name: String,
                          description: String,
                          dates: PromotionDates,
                          price: Option[String],
                          location: PromotionLocation,
                          media: MediaUrl,
                          visitors: Option[PromotionVisitors],
                          @JsonProperty("isvisitor") isVisitor: Option[Boolean] = None
                        )

object PromotionDataItem {
  def apply(promotionVisitorsShortMap: Option[Map[PromotionId, PromotionVisitorCounters]], lang: Lang)(p: Promotion): PromotionDataItem = PromotionDataItem(
    id = p.id.id,
    name = p.name.getWithDefaultKey(lang),
    description = p.description.getWithDefaultKey(lang),
    dates = PromotionDates(p.dates),
    price = p.price.map(_.getWithDefaultKey(lang)),
    location = PromotionShortLocation(p.location)(lang),
    media = p.media.head,
    visitors = promotionVisitorsShortMap map (_(p.id))
  )
}

case class PromotionDates
(
  @JsonProperty("startdate") startDate: Date,
  @JsonProperty("enddate") endDate: Option[Date],
  time: Option[Date],
  validity: Option[Int]
  )

object PromotionDates {
  import DateHelper._

  def apply(ds: Dates): PromotionDates = PromotionDates(
    startDate = ds.start,
    endDate = ds.end,
    time = ds.time,
    validity = ds.end map currentDate.daysLeftTo
  )
}

case class UserEventPromotion
(
  @JsonProp("id") id: PromotionId,
  @JsonProp("name") name: String,
  @JsonProp("dates") dates: PromotionDates,
  @JsonSkip locationId: LocationId
  )

object UserEventPromotion {

  def apply(lang: Lang)(p: Promotion): UserEventPromotion = UserEventPromotion(
    id = p.id,
    name = p.name.localized(lang) getOrElse "",
    dates = PromotionDates(p.dates),
    locationId = p.location.location
  )
}