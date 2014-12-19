package com.tooe.core.usecase

import com.tooe.core.application.Actors
import location.LocationDataActor
import com.tooe.core.db.graph._
import com.tooe.core.db.graph.msg._
import scala.concurrent.Future
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.api._
import com.tooe.core.domain._
import com.tooe.api.service.SuccessfulResponse
import com.tooe.core.util.Lang
import com.tooe.core.db.mongo.domain._
import com.tooe.core.domain.Coordinates
import com.tooe.core.domain.LocationCategoryId
import com.tooe.core.db.mongo.domain.Location
import com.tooe.core.domain.LocationId
import com.tooe.core.domain.RegionId
import com.tooe.core.domain.UserId
import com.tooe.core.domain.AdditionalLocationCategoryId
import com.tooe.api.service.RouteContext
import com.tooe.core.exceptions.NotFoundException
import akka.actor.Status.Failure
import com.tooe.api.validation.{Validatable, ValidationContext}

object LocationWriteActor {
  final val Id = Actors.LocationWrite

  case class AddLocationToFavorite(locationId: LocationId, userId: UserId)
  case class AddOwnProductCategory(locationId: LocationId, request: AddOwnCategoryRequest, ctx: RouteContext)
  case class RemoveOwnProductCategory(request: ChangeAdditionalCategoryRequest)
  case class ChangeOwnProductCategory(request: ChangeAdditionalCategoryRequest, renameParameters: RenameAdditionalCategoryParameters, ctx: RouteContext)
  case class RemoveLocationFromFavorites(locationId: LocationId, userId: UserId)
  case class IncrementFavoriteCounter(locationId: LocationId, delta: Int)
}

class LocationWriteActor extends AppActor {

  import scala.concurrent.ExecutionContext.Implicits.global
  import LocationWriteActor._

  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val locationGraphActor = lookup(LocationGraphActor.Id)
  lazy val putLocationToFavoriteGraphActor = lookup(GraphPutFavoritesActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val newsWriteActor = lookup(NewsWriteActor.Id)
  lazy val favoriteStatsActor = lookup(FavoriteStatsActor.Id)
  lazy val productWriteActor = lookup(ProductWriteActor.Id)

  def receive = {

    case AddLocationToFavorite(locationId, userId) =>
       val result = for {
         locationExists <- locationExistsCheck(locationId)
         graphFavorite <- putLocationToFavorites(userId, locationId)
       } yield {
         locationDataActor ! LocationDataActor.PutUserToUsersWhoFavorite(locationId, userId)
         incrementFavoritesCounter(locationId, userId, 1)
         favoriteStatsActor ! FavoriteStatsActor.UserFavoredAnother(userId, locationId)
         newsWriteActor ! NewsWriteActor.AddFavoriteLocation(userId, locationId)
         SuccessfulResponse
       }
      result recover  {
        case ex: GraphException => Failure(NotFoundException(ex.getMessage))
      }  pipeTo sender

    case AddOwnProductCategory(locationId, request, ctx) =>
      val result = for{
        productCategoryId <- locationDataActor.ask(LocationDataActor.AddOwnProductCategory(locationId, request.name, ctx)).mapTo[AdditionalLocationCategoryId]
      } yield AddOwnCategoryResponse(AdditionalLocationCategoryIdItem(productCategoryId))
      result pipeTo sender

    case RemoveOwnProductCategory(request) =>{
      val future = locationDataActor.ask(LocationDataActor.RemoveOwnProductCategory(request)).map(_ => SuccessfulResponse) pipeTo sender
      future onSuccess{
        case _ => removeProductCategories(request)
      }
    }
    sender ! SuccessfulResponse


    case ChangeOwnProductCategory(request, renameParameters, ctx) => {
      val future = locationDataActor.ask(LocationDataActor.ChangeAdditionalCategory(request, renameParameters, ctx))
      future onSuccess{
        case _ => updateProductCategories(request, renameParameters, ctx)
      }
    }
    sender ! SuccessfulResponse

    case RemoveLocationFromFavorites(locationId, userId) =>
       removeLocationFromFavorites(userId, locationId).map(_ => {
         locationDataActor ! LocationDataActor.RemoveUserFromUsersWhoFavorite(locationId, userId)
         incrementFavoritesCounter(locationId, userId, -1)
         favoriteStatsActor ! FavoriteStatsActor.UserRemovedFromFavorite(userId, locationId)
          SuccessfulResponse
       }) recover  {
         case ex: GraphException => Failure(NotFoundException(ex.getMessage))
       } pipeTo sender
  }

  def removeProductCategories(request: ChangeAdditionalCategoryRequest) = {
    productWriteActor ! ProductWriteActor.RemoveAdditionalProductCategory(request)
  }

  def updateProductCategories(request: ChangeAdditionalCategoryRequest,
                              renameParameters: RenameAdditionalCategoryParameters, ctx: RouteContext) = {
    productWriteActor ! ProductWriteActor.UpdateAdditionalProductCategory(request, renameParameters, ctx)
  }


  def removeLocationFromFavorites(userId: UserId, locationId: LocationId): Future[Boolean] = {
    putLocationToFavoriteGraphActor.ask(new GraphDeleteFavorite(userId, locationId)).mapTo[Boolean]
  }

  def putLocationToFavorites(userId: UserId, locationId: LocationId): Future[GraphFavorite] = {
    putLocationToFavoriteGraphActor.ask(new GraphPutFavorite(userId, locationId)).mapTo[GraphFavorite]
  }

  def incrementFavoritesCounter(locationId: LocationId, userId: UserId, delta: Int) {
    updateStatisticActor ! UpdateStatisticActor.ChangeLocationFavoritesCounters(locationId, userId, delta)
  }

  def getLocation(locationId: LocationId): Future[Location] = {
    (locationDataActor ? LocationDataActor.GetLocation(locationId)).mapTo[Location]
  }

  def locationExistsCheck(locationId: LocationId): Future[Boolean] = {
    (locationDataActor ? LocationDataActor.LocationExistsCheck(locationId)).mapTo[Boolean]
  }

}

case class UpdateLocationRequest
(
  name: Option[String],
  @JsonProperty("openinghours") openingHours: Option[String],
  description: Option[String],
  address: Option[UpdateLocationAddressItem],
  categories: Option[Seq[LocationCategoryId]],
  @JsonProperty("coords") coordinates: Option[Coordinates],
  @JsonProperty("countrycode") countryCode: Option[String],
  phone: Option[String],
  media: Option[MediaUrl],
  url: Unsetable[String]
  ) extends UnmarshallerEntity with Validatable with PhoneValidator {

  def validate(ctx: ValidationContext) {

    if(phone.isEmpty ^ countryCode.isEmpty)
      ctx.fail("Set phone and country code or unset each")
    validatePhone(ctx, phone, countryCode)

  }

}

trait PhoneValidator  {

  private val NumberPattern = """\d+"""

  def validatePhone(ctx: ValidationContext, phone: Option[String], code: Option[String]) = {
    phone.zip(code).foreach { phoneWithCode  =>
      if(!s"${phoneWithCode._1}${phoneWithCode._2}".matches(NumberPattern))
        ctx.fail("Phone and country code must contain only numbers")
    }
  }
  
}

case class UpdateLocationAddressItem
(
  @JsonProperty("regionid") regionId: Option[RegionId],
  street: Option[String]
  ) extends UnmarshallerEntity

case class AddOwnCategoryRequest(name: String) extends UnmarshallerEntity

case class AddOwnCategoryResponse(@JsonProp("productcategory") productCategory: AdditionalLocationCategoryIdItem) extends SuccessfulResponse

case class AdditionalLocationCategoryItem(categoryId: AdditionalLocationCategoryId, name: String)

object AdditionalLocationCategoryItem {
  def apply(category: AdditionalLocationCategory)(implicit lang: Lang): AdditionalLocationCategoryItem =
    AdditionalLocationCategoryItem(category.id,
                                   category.name.localized.getOrElse(""))
}

case class AdditionalLocationCategoryIdItem(id: AdditionalLocationCategoryId)

case class ChangeAdditionalCategoryRequest(locationId: LocationId, categoryId: AdditionalLocationCategoryId) extends UnmarshallerEntity

case class RenameAdditionalCategoryParameters(name: String) extends UnmarshallerEntity