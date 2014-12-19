package com.tooe.core.service

import com.tooe.api.service.LocationModerationRequest
import com.tooe.api.service.OffsetLimit
import com.tooe.core.db.mongo.converters.{PhoneConverter, PreModerationStatusConverter, DBObjectConverters, LocationMediaConverter}
import com.tooe.core.db.mongo.domain.{LocationMedia, Phone, PreModerationLocation, PreModerationStatus}
import com.tooe.core.db.mongo.query.UpdateResult.NoUpdate
import com.tooe.core.db.mongo.query._
import com.tooe.core.db.mongo.repository.PreModerationLocationRepository
import com.tooe.core.domain._
import com.tooe.core.usecase._
import com.tooe.core.usecase.location.{ModerationLocationSearchSortType, PreModerationLocationAdminSearchSortType}
import com.tooe.core.util.BuilderHelper
import com.tooe.core.util.BuilderHelper.BuilderWrapper
import com.tooe.core.util.Lang
import com.tooe.core.util.ProjectionHelper._
import java.util.Date
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.{Query, Criteria}
import org.springframework.stereotype.Service
import scala.Some
import scala.collection.JavaConverters._

trait PreModerationLocationDataService {

  def findById(id: PreModerationLocationId): Option[PreModerationLocation]

  def findByLocationId(locationId: LocationId): Option[PreModerationLocation]

  def findByAdminSearchParams(params: PreModerationLocationAdminSearchParams): Seq[PreModerationLocation]

  def countByAdminSearchParams(params: PreModerationLocationAdminSearchParams): Long

  def findOwnModerationLocations(request: ModerationLocationsSearchRequest, companies: Set[CompanyId], lang: Lang): Seq[PreModerationLocation]

  def countOwnModerationLocations(request: ModerationLocationsSearchRequest, companies: Set[CompanyId], lang: Lang): Long

  def save(pml: PreModerationLocation): PreModerationLocation
  def updateLocation(locationId: PreModerationLocationId, ulr: UpdateLocationRequest, lang: Lang): UpdateResult

  def getPublishLocationId(id: PreModerationLocationId): Option[LocationId]

  def updatePublishId(id: PreModerationLocationId, locationId: LocationId): Unit

  def updateModerationStatus(id: PreModerationLocationId, request: LocationModerationRequest, userId: AdminUserId): Unit

  def updateLifecycleStatus(id: PreModerationLocationId, status: Option[LifecycleStatusId]): UpdateResult

  def updateMediaStorageToS3(id: PreModerationLocationId, media: MediaObjectId, newMedia: MediaObjectId): Unit

  def updateMediaStorageToCDN(id: PreModerationLocationId, media: MediaObjectId): Unit

}

case class PreModerationLocationAdminSearchParams
(
  name: Option[String],
  company: Option[CompanyId],
  modstatus: Option[ModerationStatusId],
  sort: Option[PreModerationLocationAdminSearchSortType],
  offsetLimit: OffsetLimit,
  lang: Lang
  )

object PreModerationLocationAdminSearchParams {

  def apply(request: SearchPreModerationLocationsRequest, offsetLimit: OffsetLimit, lang: Lang): PreModerationLocationAdminSearchParams =
    PreModerationLocationAdminSearchParams(request.name, request.company, request.modstatus, request.sort, offsetLimit, lang)

}

@Service
class PreModerationLocationDataServiceImpl extends PreModerationLocationDataService with PreModerationStatusConverter with LocationMediaConverter with PhoneConverter {
  @Autowired var repo: PreModerationLocationRepository = _
  @Autowired var mongo: MongoTemplate = _

  import DBObjectConverters._
  import scala.collection.JavaConversions._

  val entityClass = classOf[PreModerationLocation]

  private implicit def preModerationLocationAdminSearchParams2Criteria(params: PreModerationLocationAdminSearchParams): Criteria =
    new Criteria()
      .extend(params.name)(name => _.and("ns").regex(s"^${name.toLowerCase}"))
      .extend(params.company)(companyId => _.and("cid").is(companyId.id))
      .extend(params.modstatus)(modStatus => _.and("mod.s").is(modStatus.id))

  def findById(id: PreModerationLocationId) = Option(repo.findOne(id.id))

  def findByLocationId(locationId: LocationId) = Option(repo.findByLocationId(locationId.id))

  def findByAdminSearchParams(params: PreModerationLocationAdminSearchParams) = {
    val query = Query.query(params)

    val sortField = params.sort.getOrElse(PreModerationLocationAdminSearchSortType.Default) match {
      case PreModerationLocationAdminSearchSortType.ModerationStatus => "mod.s"
      case _ => s"n.${params.lang.id}" // TODO add case sorting by company
    }

    query.asc(sortField)
    query.withPaging(params.offsetLimit)
    query.fields.exclude("ns")
    mongo.find(query, entityClass)
  }

  def countByAdminSearchParams(params: PreModerationLocationAdminSearchParams) =
    mongo.count(Query.query(params), entityClass)

  private def ownModerationLocationQuery(request: ModerationLocationsSearchRequest, companies: Set[CompanyId], lang: Lang) = {
    Query.query(new Criteria()
      .extend(request.name)(name => _.and("ns").regex(s"^${name.toLowerCase}"))
      .extend(request.moderationStatus)(modStatus => _.and("mod.s").is(modStatus.id))
      .and("cid").in(companies.map(_.id).asJavaCollection)
    )
  }

  def findOwnModerationLocations(request: ModerationLocationsSearchRequest, companies: Set[CompanyId], lang: Lang): Seq[PreModerationLocation] = {
    val sortField = request.sortField match {
      case ModerationLocationSearchSortType.Name => s"n.${lang.id}"
      case ModerationLocationSearchSortType.ModerationStatus => "mod.s"
    }
    val query = ownModerationLocationQuery(request, companies, lang).asc(sortField).withPaging(request.offsetLimit)
    query.fields.exclude("ns")
    mongo.find(query, entityClass)
  }

  def countOwnModerationLocations(request: ModerationLocationsSearchRequest, companies: Set[CompanyId], lang: Lang): Long = {
    mongo.count(ownModerationLocationQuery(request, companies, lang), entityClass)
  }

  def save(pml: PreModerationLocation) = repo.save(pml)

  def updateLocation(locationId: PreModerationLocationId, ulr: UpdateLocationRequest, lang: Lang): UpdateResult = {
    import BuilderHelper._

    val namesUpdate = ulr.name.flatMap(name => findById(locationId).map(pml => pml.copy(name = pml.name.updated(lang.id, name)).names))

    val update = new Update()
      .setOrSkip("n." + lang, ulr.name)
      .setOrSkipSeq("ns", namesUpdate)
      .setOrSkip("oh." + lang, ulr.openingHours)
      .setOrSkip("d." + lang, ulr.description)
      .extend(ulr.address)(address => _
      .setOrSkip("c.a.rid", address.regionId)
      .setOrSkip("c.a.s", address.street)
    )
      .setSkipUnset("lc", ulr.categories.map(_.map(_.id)))
      .setSkipUnset("c.a.l", ulr.coordinates)
      .extend(ulr.phone)(phone => _.setSerializeSeq("c.p", Seq(Phone(number = phone, countryCode = ulr.countryCode.getOrElse(null), purpose = Some("main")))))
      .setSerialize("mod.s", ModerationStatusId.Waiting)
      .extend(ulr.media)(media => _.setSerializeSeq("lm", Seq(LocationMedia(url = MediaObject(media.imageUrl), purpose = Some("main")))))
      .setSkipUnset("c.url", ulr.url)
    if (update.getUpdateObject.keySet().isEmpty) NoUpdate
    else mongo.updateFirst(
      Query.query(new Criteria("_id").is(locationId.id)),
      update,
      entityClass
    ).asUpdateResult
  }

  def getPublishLocationId(id: PreModerationLocationId) = {
    val query = Query.query(new Criteria("_id").is(id.id)).extendProjection(Set("puid"))
    Option(mongo.findOne(query, entityClass)).flatMap(_.publishedLocation)
  }

  def updatePublishId(id: PreModerationLocationId, locationId: LocationId) {
    val query = Query.query(new Criteria("_id").is(id.id))
    val update = new Update().setSerialize("puid", locationId)
    mongo.updateFirst(query, update, entityClass)
  }

  def updateModerationStatus(id: PreModerationLocationId, request: LocationModerationRequest, userId: AdminUserId) {

    val query = Query.query(new Criteria("_id").is(id.id))
    val update = new Update().setSerialize("mod", PreModerationStatus(request.status, request.message, Some(userId), Some(new Date)))
    mongo.updateFirst(query, update, entityClass)
  }

  def updateLifecycleStatus(id: PreModerationLocationId, status: Option[LifecycleStatusId]) = {
    val query = Query.query(new Criteria("_id").is(id.id))
    val update = new Update().setSkipUnset("lfs", status.map(v => Unsetable.Update(v)).getOrElse(Unsetable.Unset))
    mongo.updateFirst(query, update, entityClass).asUpdateResult
  }

  def updateMediaStorageToS3(id: PreModerationLocationId, media: MediaObjectId, newMedia: MediaObjectId) {
    val query = Query.query(new Criteria("_id").is(id.id).and("lm.u.mu").is(media.id))
    val update = new Update().set("lm.$.u.t", UrlType.s3.id).set("lm.$.u.mu", newMedia.id)
    mongo.updateFirst(query, update, entityClass)
  }

  def updateMediaStorageToCDN(id: PreModerationLocationId, media: MediaObjectId) {
    val query = Query.query(new Criteria("_id").is(id.id).and("lm.u.mu").is(media.id))
    val update = new Update().unset("lm.$.u.t")
    mongo.updateFirst(query, update, entityClass)
  }

}
