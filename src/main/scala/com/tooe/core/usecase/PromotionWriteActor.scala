package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import java.util.Date
import com.tooe.api.service.{ExecutionContextProvider, SuccessfulResponse, RouteContext}
import com.tooe.core.util.Lang
import location.LocationDataActor
import location.LocationDataActor.GetLocation
import com.tooe.core.usecase.promotion.PromotionDataActor
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain.promotion.{Location => LocationPromo, Dates}
import com.tooe.core.db.mongo.domain._
import com.tooe.core.db.mongo.domain
import concurrent.Future

object PromotionWriteActor {
  final val Id = Actors.PromotionWrite

  case class SavePromotion(request: SavePromotionRequest, ctx: RouteContext)
  case class ChangePromotion(promotionId: PromotionId, request: PromotionChangeRequest, lang: Lang)
}

class PromotionWriteActor extends AppActor with ExecutionContextProvider {
  lazy val promotionDataActor = lookup(PromotionDataActor.Id)
  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)

  import PromotionWriteActor._

  def receive = {

    case SavePromotion(request, ctx) =>
      implicit val lang = ctx.lang
      val result = for {
        location <- getLocation(request)
        regionId = location.contact.address.regionId
        promotion <- savePromotion(request, location, regionId)
      } yield {
        //TODO update Locaction.hasPromotions
        updateStatisticActor ! UpdateStatisticActor.ChangePromotionsCounter(regionId, 1)
        SavePromotionResponse(PromotionIdItem(promotion.id))
      }
      result pipeTo sender

    //TODO on delete promotion => update Locaction.hasPromotions

    case ChangePromotion(promotionId, request, lang) =>
      promotionDataActor !  PromotionDataActor.UpdatePromotion(promotionId, request, lang)
      sender ! SuccessfulResponse
  }

  def savePromotion(request: SavePromotionRequest, l: Location, regionId: RegionId)(implicit lang: Lang): Future[Promotion] = {
    promotionDataActor.ask(PromotionDataActor.SavePromotion(
      Promotion(
        name = ObjectMap(request.name),
        description = ObjectMap(request.description),
        additionalInfo = request.addInfo.map(ObjectMap(_)),
        media = request.media.toSeq,
        dates = Dates(start = request.startDate, end = request.endDate, time = request.time, period = PromotionPeriod.Day), //TODO Dates.Period
        price = request.price map (x => ObjectMap(x)),
        location = LocationPromo(request.locationId, l.name, regionId, l.locationCategories),
        visitorsCount = 0
      )
    )
    ).mapTo[Promotion]
  }

  def getLocation(request: SavePromotionRequest): Future[Location] = {
    locationDataActor.ask(GetLocation(request.locationId)).mapTo[domain.Location]
  }
}

case class ChangePromotionStatusRequest(@JsonProperty("status") status: PromotionStatus) extends UnmarshallerEntity

case class SavePromotionRequest
(
  name: String,
  description: String,
  @JsonProperty("addinfo") addInfo: Option[String],
  @JsonProperty("startdate") startDate: Date,
  @JsonProperty("enddate") endDate: Option[Date],
  time: Option[Date],
  period: Option[String],
  price: Option[String],
  @JsonProperty("locationid") locationId: LocationId,
  media: Option[MediaUrl]
  ) extends UnmarshallerEntity

case class SavePromotionResponse(promotion: PromotionIdItem) extends SuccessfulResponse
case class PromotionIdItem(id: PromotionId)

case class PromotionChangeRequest(name: Option[String],
                                  description: Option[String],
                                  @JsonProperty("addinfo") additionalInformation: Option[String],
                                  @JsonProperty("startdate") startDate: Option[Date],
                                  @JsonProperty("enddate") endDate: Unsetable[Date],
                                  time: Unsetable[Date],
                                  period: Unsetable[PromotionPeriod],
                                  price: Option[String],
                                  media: Option[MediaUrl]) extends UnmarshallerEntity