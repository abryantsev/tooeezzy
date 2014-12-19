package com.tooe.core.service

import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.LocationRepository
import org.springframework.stereotype.Service
import com.tooe.core.util.{Lang, BuilderHelper, ProjectionHelper}
import com.tooe.core.usecase.location._
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query._
import org.springframework.data.mongodb.core.geo.Metrics
import com.tooe.core.db.mongo.query._
import com.mongodb.BasicDBObject
import com.tooe.core.main.SharedActorSystem
import com.tooe.extensions.scala.Settings
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain._
import com.javadocmd.simplelatlng.LatLngTool
import com.javadocmd.simplelatlng.util.LengthUnit
import java.util.regex.Pattern
import scala.collection.JavaConverters._
import org.springframework.data.domain.Sort
import com.tooe.core.db.mongo.converters._
import com.tooe.core.usecase._
import scala.Some
import com.tooe.core.db.mongo.query.LocalizedField

trait LocationDataService extends DataService[Location, ObjectId] {

  def getLocationsSearchByCoordinates(implicit request: LocationsSearchRequest, lang: Lang): LocationSearchResult
  
  def find(coordinates: (Double, Double), radiusInMeters: Int): Seq[Location]

  def findOne(locationId: LocationId): Option[Location]

  def getLocations(locationIds: Seq[LocationId],  usersCoordinates: Option[Coordinates]): Seq[LocationWithDistance]

  def getLocations(locationIds: Seq[LocationId]): Seq[Location]

  def addOwnCategory(locationId: LocationId, categoryId: AdditionalLocationCategoryId, categoryName: String)(implicit lang: Lang): UpdateResult

  def addPhotoToLocation(locationId: LocationId, photoId: LocationPhotoId): Unit

  def updateLocationPhotos(locationId: LocationId, photoId: Seq[LocationPhotoId]): Unit

  def removeAdditionalCategory(locationId: LocationId, categoryId: AdditionalLocationCategoryId): Unit

  def deleteLocation(locationId: LocationId): Unit

  def getStatistics(locationId: LocationId): Option[LocationCounters]

  def changeStatistic(locationId: LocationId, updater: UpdateLocationStatistic): Unit

  def renameAdditionalCategory(request: ChangeAdditionalCategoryRequest, renameParameters: RenameAdditionalCategoryParameters, lang: Lang): Unit

  def putUserToUsersWhoFavorite(locationId: LocationId, userId: UserId)

  def removeUserFromUsersWhoFavorite(locationId: LocationId, userId: UserId)

  def getLocationsForInvitation(request: GetLocationsForInvitationRequest, lang: Lang): LocationsWithCountResult

  def setPromotionsFlag(lid: LocationId, flag: Option[Boolean]): Unit

  def addLocationsToChain(id: LocationsChainId, locations: Seq[LocationId]): UpdateResult

  def getLocationsByChain(request: GetLocationsByChainRequest, lang: Lang): LocationsWithCountResult

  def getLocationsForCheckin(request: GetLocationsForCheckinRequest, lang: Lang): LocationsWithCountResult

  def getFavoriteLocations(request: GetFavoriteLocationsRequest, locationIds: Seq[LocationId], lang: Lang): LocationsWithCountResult

  def updateLocation(id: LocationId, preModerationLocation: PreModerationLocation): Unit

  def getMedia(id: LocationId): Option[Seq[MediaObjectId]]

  def updateMediaStorageToS3(id: LocationId, media: MediaObjectId, newMedia: MediaObjectId): Unit

  def updateMediaStorageToCDN(id: LocationId, media: MediaObjectId): Unit

  def isLocationExist(id: LocationId): Boolean

  def updateLifecycleStatus(id: LocationId, status: Option[LifecycleStatusId]): UpdateResult

  def getLocationForAdmin(locationId: LocationId): Option[Location]

  def getActiveLocationOrByLifeCycleStatuses(locationId: LocationId, statuses: Seq[LifecycleStatusId]): Option[Location]

  def addPhotoAlbumToLocation(location: LocationId, album: LocationPhotoAlbumId)

  def deletePhotoAlbumFromLocation(location: LocationId, album: LocationPhotoAlbumId)

}

@Service
class LocationDataServiceImpl extends LocationDataService with DataServiceImpl[Location, ObjectId] with LocationContactConverter with LocationAdditionalCategoryConverter with LocationMediaConverter {
  @Autowired var repo: LocationRepository = _
  @Autowired var mongo: MongoTemplate = _

  val settings = Settings(SharedActorSystem.sharedMainActorSystem)
  import settings._

  val entityClass = classOf[Location]

  final val HalfOfEarthEquator = 20020636
  final val DefaultLimit = 20

  def findOne(locationId: LocationId): Option[Location] = {
    val query = Query.query(new Criteria("_id").is(locationId.id).and("lfs").exists(false))
    query.fields.exclude("ns")
    Option(mongo.findOne(query, entityClass))
  }

  def getLocationForAdmin(locationId: LocationId): Option[Location] = {
    val query = Query.query(new Criteria("_id").is(locationId.id))
    query.fields.exclude("ns")
    Option(mongo.findOne(query, entityClass))
  }


  def getActiveLocationOrByLifeCycleStatuses(locationId: LocationId, statuses: Seq[LifecycleStatusId]): Option[Location] = {
    val query = Query.query(new Criteria("_id").is(locationId.id).orOperator(new Criteria("lfs").exists(false), new Criteria("lfs").in(statuses.map(_.id).asJavaCollection)))
    query.fields.exclude("ns")
    Option(mongo.findOne(query, entityClass))
  }

  def deleteLocation(locationId: LocationId) = repo.delete(locationId.id)

  def getLocationsForCheckin(request: GetLocationsForCheckinRequest, lang: Lang): LocationsWithCountResult = {

    def findLocationsNearSphere = {
      repo.searchNearSphere(request.longitude, request.latitude, GeoSearch.LocationsForCheckinSearch.RadiusMax).asScala.toSeq
    }
    def countAllLocations = repo.searchGeoWithinCount(
      longitude = request.longitude,
      latitude = request.latitude,
      radius = radiusInRadians(GeoSearch.LocationsForCheckinSearch.RadiusMax)
    )

    val locationsCount = if(request.responseIncludesLocationsQty) Option(countAllLocations) else None

    LocationsWithCountResult(findLocationsNearSphere, locationsCount)
  }

  //TODO divide to get and count methods
  def getFavoriteLocations(request: GetFavoriteLocationsRequest, locationIds: Seq[LocationId], lang: Lang): LocationsWithCountResult = {
    lazy val filteringQuery = Query.query(new Criteria("c.a.rid").is(request.region.id)
      .and("lc").in(request.categoryId.id)
      .and("_id").in(locationIds.map(_.id).asJavaCollection)
    )

    //todo check conditions in read actor, not here
    val locationsCount = if(request.responseIncludesLocationsQty) Option(mongo.count(filteringQuery, entityClass).toInt) else None
    val locations = if(request.responseIncludesLocations) {
      filteringQuery.fields.exclude("ns")
      mongo.find(filteringQuery.asc(s"n.${lang.id}")
        .withPaging(request.offsetLimit), entityClass).asScala.toSeq
    } else Nil

    LocationsWithCountResult(locations, locationsCount)
  }

  def getLocationsByChain(request: GetLocationsByChainRequest, lang: Lang): LocationsWithCountResult = {
    import BuilderHelper._
    val filteringQuery = Query.query(new Criteria("c.a.rid").is(request.region.id)
      .and("lcid").is(request.locationChain.id)
      .extend(request.hasPromo)(hasPromo => _.and("pf").is(hasPromo))
      .and("lfs").exists(false)
    )
    filteringQuery.fields.exclude("ns")

    val locationsCount = if(request.responseIncludesLocationsQty) Some(mongo.count(filteringQuery, entityClass).toInt) else None
    val locations = mongo.find(filteringQuery.asc(s"n.${lang.id}")
      .withPaging(request.offsetLimit), entityClass).asScala.toSeq

    LocationsWithCountResult(locations, locationsCount)
  }

  def getLocationsForInvitation(request: GetLocationsForInvitationRequest, lang: Lang): LocationsWithCountResult = {
    import BuilderHelper._
    val filteringQuery= Query.query(new Criteria("c.a.rid").is(request.region.id)
      .extend(request.category)(category => _.and("lc").in(category.id))
      .extend(request.name)(name => _.and("ns").in(Pattern.compile(s"^${name.toLowerCase}")))
      .extend(request.userIds)(ids => _.and("uf").in(ids.map(_.id).asJavaCollection))
      .and("lfs").exists(false))
    filteringQuery.fields.exclude("ns")

    val locationsCount = if(request.responseIncludesLocationsQty) Option(mongo.count(filteringQuery, entityClass).toInt) else None
    val locations = mongo.find(filteringQuery
      .sort(request.sortType match {
      case NamePopularitySortType.Name => new Sort(Sort.Direction.ASC, s"n.${lang.id}")
      case NamePopularitySortType.Popularity => new Sort(Sort.Direction.ASC, "st.sc")
    })
    .withPaging(request.offsetLimit), entityClass).asScala.toSeq

    LocationsWithCountResult(locations, locationsCount)
  }

  def getLocationsSearchByCoordinates(implicit request: LocationsSearchRequest, lang: Lang): LocationSearchResult = {
    import BuilderHelper._
    import ProjectionHelper._

    lazy val projectionFields = Option(request.entities.flatMap(_.fields.map(_.id)))

    def filterQuery(criteria: Criteria = new Criteria()) = Query.query(criteria
        .and("lc").in(request.category.id).and("lfs").exists(false)

      )
      .withPaging(request.offsetLimit)
      .extend(projectionFields)(fields => _.extendProjection(fields))

    def findLocationsNearSphere(implicit request: LocationsSearchRequest) = {
      val criteria = Criteria.where("c.a.l").nearSphere(request.point).maxDistance(request.finalRadius)
      val query = filterQuery(criteria).limit(GeoSearch.LocationSearch.ResultCount)
      mongo.find(query, entityClass).asScala.toSeq
    }

    def countAllLocations(implicit request: LocationsSearchRequest) = repo.findLocationsGeoWithinCount(
        longitude = request.longitude,
        latitude = request.latitude,
        radius = radiusInRadians(request.finalRadius),
        categoryId = request.category.id
      )

    @deprecated("use Coordinates.distanceKm instead")
    def distance(location: Location) =
      LatLngTool.distance(request.latLng, location.contact.address.coordinates.toLatLng, LengthUnit.KILOMETER)

    val locationsWithDistance = (request.sortType match {
        case LocationSearchSortType.Distance => findLocationsNearSphere(request)
        case sortType => repo.findLocationsGeoWithin(
          longitude = request.longitude,
          latitude = request.latitude,
          radius = radiusInRadians(request.finalRadius),
          categoryId = request.category.id,
          fields =  ProjectionHelper.generateProjectionDBObject(projectionFields.toSet.flatten).append("ns", "0"),
          pageable = sortType.getSort(lang).map(sort => SkipLimitSort(request.offsetLimit, sort)).get
        ).asScala.toSeq
    }) map(l => LocationWithDistance(l, Option(distance(l))))
    val locationsCount = if(request.responseIncludesLocationsQty) Option(countAllLocations) else None

    LocationSearchResult(locationsWithDistance, locationsCount)
  }

  def find(coordinates: (Double, Double), radiusInMeters: Int) = {
    val (lng, lat) = coordinates
    repo.searchGeoWithin(lng, lat, radiusInRadians(radiusInMeters)).asScala.toSeq
  }


  def radiusInRadians(radiusInMeters: Int): Double = {
    radiusInMeters * Math.PI / HalfOfEarthEquator
  }

  def getLocations(locationIds: Seq[LocationId], usersCoordinates: Option[Coordinates]): Seq[LocationWithDistance] = {
    val result = usersCoordinates.map(ll => {
      val geoNearQuery = mongo.geoNear(
        NearQuery.near(ll.longitude, ll.latitude).maxDistance(3, Metrics.KILOMETERS)
          .query(Query.query(new Criteria("_id").in(locationIds.map(_.id).asJavaCollection)))
          .num(10000) //TODO pass limit as a parameter, without num, it will return zero records due to .query method
          .spherical(true),
        classOf[Location]
      )
      geoNearQuery.getContent.asScala.toSeq.map(resultItem => LocationWithDistance(resultItem.getContent, Option(resultItem.getDistance.getNormalizedValue)))
    }).getOrElse( repo.getLocations(locationIds.map(_.id)).asScala.toSeq.map( locations => LocationWithDistance(locations, None)) )
    result
  }

  def getLocations(locationIds: Seq[LocationId]) = repo.getLocations(locationIds.map(_.id)).asScala

  def addOwnCategory(locationId: LocationId, categoryId: AdditionalLocationCategoryId,categoryName: String)(implicit lang: Lang): UpdateResult =
    mongo.updateFirst(
      Query.query(new Criteria("_id").is(locationId.id)),
      (new Update).addToSet("lac", new BasicDBObject("cid", categoryId.id).append("n", new BasicDBObject(lang.id, categoryName))),
      classOf[Location]
    ).asUpdateResult


  def removeAdditionalCategory(locationId: LocationId, categoryId: AdditionalLocationCategoryId) {
    mongo.updateFirst(
      Query.query(new Criteria("_id").is(locationId.id)),
      (new Update).pull("lac", new BasicDBObject("cid", categoryId.id)),
      classOf[Location]
    )
  }

  def renameAdditionalCategory(request: ChangeAdditionalCategoryRequest, renameParameters: RenameAdditionalCategoryParameters, lang: Lang) {
    mongo.updateFirst(
      Query.query(new Criteria("_id").is(request.locationId.id).and("lac.cid").is(request.categoryId.id)),
      (new Update).set("lac.$." + LocalizedField("n", lang).value, renameParameters.name),
      classOf[Location]
    )
  }

  def addPhotoToLocation(locationId: LocationId, photoId: LocationPhotoId) {
    val query = Query.query(new Criteria("id").is(locationId.id))
    val update = (new Update).push("lp", new BasicDBObject("$each", java.util.Arrays.asList(photoId.id)).append("$slice", -6) )
    mongo.updateFirst(query, update, classOf[Location])
  }

  def updateLocationPhotos(locationId: LocationId, photoIds: Seq[LocationPhotoId]) {
    val query = Query.query(new Criteria("id").is(locationId.id))
    val update = (new Update).set("lp", photoIds.take(6) map (_.id) )
    mongo.updateFirst(query, update, entityClass)
  }

  def getStatistics(locationId: LocationId) = {
    import ProjectionHelper._
    val query = Query.query(new Criteria("id").is(locationId.id).and("lfs").exists(false))
      .extendProjection(Seq("st"))

    Option(mongo.findOne(query, entityClass)).map(_.statistics)
  }

  def changeStatistic(locationId: LocationId, updater: UpdateLocationStatistic): Unit = {
    val query = Query.query(new Criteria("_id").in(locationId.id))
    val update = UpdateLocationStatisticHelper.changeStatistic(new Update(), updater)
    mongo.updateFirst(query, update, entityClass)
  }

  def putUserToUsersWhoFavorite(locationId: LocationId, userId: UserId): Unit = {
    val query = Query.query(new Criteria("id").is(locationId.id))
    val update = (new Update).addToSet("uf", userId.id )
    mongo.updateFirst(query, update, entityClass)
  }
  def removeUserFromUsersWhoFavorite(locationId: LocationId, userId: UserId): Unit = {
    val query = Query.query(new Criteria("id").is(locationId.id))
    val update = (new Update).pull("uf", userId.id )
    mongo.updateFirst(query, update, entityClass)
  }
  def setPromotionsFlag(lid: LocationId, flag: Option[Boolean]): Unit = {
    val query = Query.query(new Criteria("id").is(lid.id))
    val update = (new Update).set("pf", flag)
    mongo.updateFirst(query, update, entityClass)
  }

  def addLocationsToChain(id: LocationsChainId, locations: Seq[LocationId]) =
    mongo.updateFirst(
      Query.query(new Criteria("_id").in(locations.map(_.id).asJavaCollection)),
      new Update().set("lcid", id.id),
      entityClass
    ).asUpdateResult

  def updateLocation(id: LocationId, preModerationLocation: PreModerationLocation) {
    import DBObjectConverters._
    import BuilderHelper._

    val query = Query.query(new Criteria("id").is(id.id))
    val update = new Update().setSerialize("cid", preModerationLocation.companyId)
                            .extend(preModerationLocation.locationsChainId)(chainId => _.setSerialize("lcid", chainId))
                            .setSerialize("n", preModerationLocation.name)
                            .setSerialize("d", preModerationLocation.description)
                            .setSerializeSeq("ns", preModerationLocation.names)
                            .setSerialize("oh", preModerationLocation.openingHours)
                            .setSerialize("c", preModerationLocation.contact)
                            .setSerializeSeq("lc", preModerationLocation.locationCategories)
                            .setSerializeSeq("lac", preModerationLocation.additionalLocationCategories)
                            .setSerializeSeq("lm", preModerationLocation.locationMedia)
    mongo.updateFirst(query, update, entityClass)
  }

  def getMedia(id: LocationId) = {
    import com.tooe.core.util.ProjectionHelper._
    val query = Query.query(new Criteria("_id").is(id.id)).extendProjection(Set("lm"))
    Option(mongo.findOne(query, entityClass)).map(_.locationMedia.map(_.url.url))
  }

  def updateMediaStorageToS3(id: LocationId, media: MediaObjectId, newMedia: MediaObjectId) {
    val query = Query.query(new Criteria("_id").is(id.id).and("lm.u.mu").is(media.id))
    val update = new Update().set("lm.$.u.t", UrlType.s3.id).set("lm.$.u.mu", newMedia.id)
    mongo.updateFirst(query, update, entityClass)
  }

  def updateMediaStorageToCDN(id: LocationId, media: MediaObjectId) {
    val query = Query.query(new Criteria("_id").is(id.id).and("lm.u.mu").is(media.id))
    val update = new Update().unset("lm.$.u.t")
    mongo.updateFirst(query, update, entityClass)
  }
  def isLocationExist(id: LocationId): Boolean = {
    val query = Query.query(new Criteria("_id").is(id.id))
    mongo.exists(query, entityClass)
  }

  def updateLifecycleStatus(id: LocationId, status: Option[LifecycleStatusId]) = {
    import DBObjectConverters._
    val query = Query.query(new Criteria("_id").is(id.id))
    val update = new Update().setSkipUnset("lfs", status.map(v => Unsetable.Update(v)).getOrElse(Unsetable.Unset))
    mongo.updateFirst(query, update, entityClass).asUpdateResult
  }

  def addPhotoAlbumToLocation(location: LocationId, album: LocationPhotoAlbumId): Unit = {
    val query = Query.query(new Criteria("_id").is(location.id))
    val update = new Update().addToSet("pa", album.id)
    mongo.updateFirst(query, update, entityClass)
  }

  def deletePhotoAlbumFromLocation(location: LocationId, album: LocationPhotoAlbumId): Unit = {
    val query = Query.query(new Criteria("_id").is(location.id))
    val update = new Update().pull("pa", album.id)
    mongo.updateFirst(query, update, entityClass)
  }
}
