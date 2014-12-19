package com.tooe.core.usecase.location

import com.tooe.core.usecase._
import com.tooe.core.application.Actors
import com.tooe.core.util.{InfoMessageHelper, Lang}
import scala.concurrent.Future
import com.tooe.api.service._
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain._
import com.tooe.core.usecase.admin_user_event.AdminUserEventDataActor
import com.tooe.core.usecase.UpdateLocationRequest
import com.tooe.core.db.mongo.domain.Phone
import com.tooe.core.db.graph.{GraphPutLocation, LocationGraphActor}
import com.tooe.core.db.graph.msg.GraphPutLocationAcknowledgement
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.usecase.urls.{UrlsDataActor, UrlsWriteActor}
import com.tooe.core.usecase.DeleteMediaServerActor.DeletePhotoFile
import com.tooe.core.usecase.admin_user.AdminUserDataActor
import com.tooe.core.db.mongo.query.UpdateResult

object PreModerationLocationWriteActor {
  final val Id = Actors.PreModerationLocationWriteActor

  case class SaveLocation(companyId: CompanyId, request: SaveLocationRequest, lang: Lang)

  case class UpdateLocation(locationId: PreModerationLocationId, ulr: UpdateLocationRequest, ctx: RouteContext)

  case class UpdateModerationStatus(locationId: PreModerationLocationId, request: LocationModerationRequest, userId: AdminUserId, ctx: RouteContext)

}

class PreModerationLocationWriteActor extends AppActor {

  import PreModerationLocationWriteActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val regionActor = lookup(RegionActor.Id)
  lazy val countryActor = lookup(CountryReadActor.Id)
  lazy val adminUserDataActor = lookup(AdminUserDataActor.Id)
  lazy val preModerationLocationDataActor = lookup(PreModerationLocationDataActor.Id)
  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val infoMessageActor = lookup(InfoMessageActor.Id)
  lazy val adminUserEventDataActor = lookup(AdminUserEventDataActor.Id)
  lazy val locationGraphActor = lookup(LocationGraphActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val urlsWriteActor = lookup(UrlsWriteActor.Id)
  lazy val deleteMediaServerActor = lookup(DeleteMediaServerActor.Id)
  lazy val urlsDataActor = lookup(UrlsDataActor.Id)

  lazy val locationInfoMessageKey = "location_approved_by_moderator"

  def receive = {
    case SaveLocation(companyId, request, lang) =>
      val result = for {
        (regionItem, countryId) <- getRegionItemWithCountryId(request.address.regionId, lang)
        countryItem <- getCountryItem(countryId, lang)
        newLocation = createLocation(companyId,request, countryItem, regionItem, lang)
        location <- saveLocation(newLocation)
      } yield {
        addModerationLocationUrls(location.id, location.locationMedia.map(_.url.url))
        SaveLocationResponse(LocationResponseItem(location.id))
      }
      result pipeTo sender

    case UpdateLocation(locationId, ulr, ctx) =>
      getModerationLocation(locationId).flatMap {
        case l if l.moderationStatus.status == ModerationStatusId.Waiting  =>
          InfoMessageHelper.throwAppExceptionById("update_location_with_waiting_status")(ctx.lang)
        case location =>
          preModerationLocationDataActor ! PreModerationLocationDataActor.UpdateLocation(locationId, ulr, ctx)
          ulr.media.map {
            media =>
              addModerationLocationUrls(location.id, Seq(MediaObjectId(media.imageUrl)))
              urlsDataActor ! UrlsDataActor.DeleteUrlsByEntityAndUrl(location.locationMedia.map(m => locationId.id -> m.url.url))
          }
        Future.successful(location.publishedLocation.map {
          publishId =>
            PublishLocationResponse(PublishLocationId(publishId))
        }.getOrElse(SuccessfulResponse))
      } pipeTo sender

    case UpdateModerationStatus(preModerationLocationId, request, userId, ctx) =>
      def publishAdminNews(location: PreModerationLocation) {
        implicit val lang = ctx.lang
        getInfoMessage(locationInfoMessageKey, lang).map(_.format(location.name.localized.getOrElse(""))).foreach(message =>
          findClientsAndAgents(location.companyId).foreach(_.foreach {
            admin =>
              adminUserEventDataActor ! AdminUserEventDataActor.SaveAdminEvent(AdminUserEvent(adminUserId = admin.id, message = message))
          })
        )
      }
      request.status match {
        case ModerationStatusId.Active =>
          (for {
            moderatedLocation <- getModerationLocation(preModerationLocationId)
            locationId <- updateLocation(moderatedLocation)
            location <- getLocationWithAnyLifeCycleStatus(locationId)
          } yield {
            updateModerationStatus(preModerationLocationId, request, userId)
            publishAdminNews(moderatedLocation)
            LocationModerationUpdateResponse(Some(LocationPublishId(locationId)))
          }) pipeTo sender
        case _ =>
          (preModerationLocationDataActor ? PreModerationLocationDataActor.GetPublishLocationId(preModerationLocationId)).mapTo[Option[LocationId]] map {
            publishLocationId =>
              updateModerationStatus(preModerationLocationId, request, userId)
              publishLocationId.foreach(_ => getModerationLocation(preModerationLocationId).foreach(publishAdminNews))
              LocationModerationUpdateResponse(publishLocationId.map(LocationPublishId))
          } pipeTo sender

      }
  }

  def getLocationWithAnyLifeCycleStatus(id: LocationId): Future[Location] =
    (locationDataActor ? LocationDataActor.GetLocationWithAnyLifeCycleStatus(id)).mapTo[Location]

  def updateLocationLifecycleStatus(id: LocationId, status: Option[LifecycleStatusId]) =
    locationDataActor.ask(LocationDataActor.UpdateLifecycleStatus(id, status)).mapTo[UpdateResult]

  def updateLocationModLifecycleStatus(id: PreModerationLocationId, status: Option[LifecycleStatusId]) =
    preModerationLocationDataActor.ask(PreModerationLocationDataActor.UpdateLifecycleStatus(id, status)).mapTo[UpdateResult]

  def getInfoMessage(key: String, lang: Lang) =
    infoMessageActor.ask(InfoMessageActor.GetMessage(key, lang.id)).mapTo[String]

  def findClientsAndAgents(id: CompanyId) =
    adminUserDataActor.ask(AdminUserDataActor.FindByRolesForCompany(id, Seq(AdminRoleId.Client, AdminRoleId.Agent))).mapTo[Seq[AdminUser]]

  def getModerationLocation(id: PreModerationLocationId) =
    (preModerationLocationDataActor ? PreModerationLocationDataActor.FindLocationById(id)).mapTo[PreModerationLocation]

  def updateModerationStatus(id: PreModerationLocationId, request: LocationModerationRequest, userId: AdminUserId) {
    preModerationLocationDataActor ! PreModerationLocationDataActor.UpdateModerationStatus(id, request, userId)
  }

  def updateLocation(preModeratedLocation: PreModerationLocation): Future[LocationId] =
    preModeratedLocation.publishedLocation.map {
      locationId =>
        (locationDataActor ? LocationDataActor.GetLocation(locationId)).mapTo[Location].map {
          location =>
            val mediaForDelete = location.locationMedia.map(_.url.url).toSet -- preModeratedLocation.locationMedia.map(_.url.url).toSet
            deleteMediaServerActor ! DeletePhotoFile(mediaForDelete.map(m => ImageInfo(m.id, ImageType.location, locationId.id)).toSeq)
            val newPhones = preModeratedLocation.contact.phones
            val activationPhone = location.contact.activationPhone
            val locationContacts = preModeratedLocation.contact.copy(phones = newPhones ++ activationPhone.toSeq)
            locationDataActor ! LocationDataActor.UpdateLocation(locationId, preModeratedLocation.copy(contact = locationContacts))
            addLocationUrls(locationId, preModeratedLocation.locationMedia.map(_.url.url))
            locationId
        }
    } getOrElse {
      (locationDataActor ? LocationDataActor.SaveLocation(ModeratedLocationToLocation(preModeratedLocation))).mapTo[Location].map {
        location =>
          preModerationLocationDataActor ! PreModerationLocationDataActor.UpdatePublishId(preModeratedLocation.id, location.id)
          putLocationToGraph(location.id)
          updateStatisticActor ! UpdateStatisticActor.ChangeLocationsCounter(location.contact.address.regionId, 1, Some(location.contact.address.countryId))
          addLocationUrls(location.id, location.locationMedia.map(_.url.url))
          location.id
      }
    }

  def createLocation(companyId: CompanyId, request: SaveLocationRequest, countryItem: CountryDetailsItem, regionItem: RegionItem, lang: Lang): PreModerationLocation =
    PreModerationLocation(
      companyId = companyId,
      locationsChainId = None,
      name = Map(lang -> request.name),
      description = Map(lang -> request.description),
      contact = LocationContact(
        address = LocationAddress(
          coordinates = request.coordinates,
          regionId = regionItem.id,
          regionName = regionItem.name,
          countryId = countryItem.id,
          country = countryItem.name,
          street = request.address.street
        ),
        phones = request.phone.map(p => Phone(number = p, countryCode = request.countryCode.getOrElse(""), purpose = Some("main"))).toSeq,
        url = request.url
      ),
      openingHours = Map(lang -> request.openingHours),
      locationCategories = request.categories,
      lifecycleStatusId = Option(LifecycleStatusId.Deactivated),
      locationMedia = request.media.map(m => Seq(LocationMedia(url = MediaObject(m.imageUrl), purpose = Some("main")))).getOrElse(Nil)
    )

  def getRegionItemWithCountryId(id: RegionId, lang: Lang): Future[(RegionItem, CountryId)] =
    (regionActor ? RegionActor.GetRegionItemWithCountryId(id, lang)).mapTo[(RegionItem, CountryId)]

  def getCountryItem(id: CountryId, lang: Lang): Future[CountryDetailsItem] =
    (countryActor ? CountryReadActor.GetCountryItem(id, lang)).mapTo[CountryDetailsItem]

  def saveLocation(location: PreModerationLocation): Future[PreModerationLocation] = {
    preModerationLocationDataActor.ask(PreModerationLocationDataActor.SaveLocation(location)).mapTo[PreModerationLocation]
  }

  def ModeratedLocationToLocation(preModeratedLocation: PreModerationLocation) =
    Location(
      id = LocationId(),
      companyId = preModeratedLocation.companyId,
      locationsChainId = preModeratedLocation.locationsChainId,
      name = preModeratedLocation.name,
      description = preModeratedLocation.description,
      openingHours = preModeratedLocation.openingHours,
      contact = preModeratedLocation.contact,
      locationCategories = preModeratedLocation.locationCategories,
      additionalLocationCategories = preModeratedLocation.additionalLocationCategories,
      locationMedia = preModeratedLocation.locationMedia,
      lifecycleStatusId = preModeratedLocation.lifecycleStatusId
    )

  def putLocationToGraph(locationId: LocationId): Future[GraphPutLocationAcknowledgement] = {
    locationGraphActor.ask(new GraphPutLocation(locationId)).mapTo[GraphPutLocationAcknowledgement]
  }

  def addModerationLocationUrls(id: PreModerationLocationId, urls: Seq[MediaObjectId]) {
    urlsWriteActor ! UrlsWriteActor.AddModerationLocationUrl(id, urls)
  }

  def addLocationUrls(id: LocationId, urls: Seq[MediaObjectId]) {
    urlsWriteActor ! UrlsWriteActor.AddLocationUrl(id, urls)
  }

}

case class SaveLocationResponse(@JsonProperty("locationsmoderation") location: LocationResponseItem) extends SuccessfulResponse

case class LocationResponseItem(id: PreModerationLocationId)

case class PublishLocationResponse(location: PublishLocationId) extends SuccessfulResponse

case class PublishLocationId(id: LocationId)

case class LocationModerationUpdateResponse(location: Option[LocationPublishId]) extends SuccessfulResponse

case class LocationPublishId(id: LocationId)
