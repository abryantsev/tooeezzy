package com.tooe.core.service

import org.springframework.stereotype.Service
import com.tooe.core.db.mongo.repository.PresentRepository
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.domain._
import scala.collection.JavaConverters._
import com.tooe.api.service.{SentPresentsRequest, OffsetLimit, GetPresentParameters}
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import com.tooe.core.util.BuilderHelper._
import com.tooe.core.db.mongo.query._
import com.tooe.core.util.DateHelper
import com.tooe.core.usecase.present.PresentAdminSearchSortType
import com.tooe.core.db.mongo.domain.Present
import java.math.BigInteger
import com.tooe.core.db.mongo.converters.DBCommonConverters

trait PresentDataService {
  def save(entity: Present): Unit

  def find(id: PresentId): Option[Present]

  def find(ids: Set[PresentId]): Seq[Present]

  def getUserPresents(userId: UserId, parameters: GetPresentParameters, offsetLimit: OffsetLimit): Seq[Present]

  def userPresentsCount(userId: UserId, parameters: GetPresentParameters): Long

  def activatePresent(id: PresentId): UpdateResult

  def findByCode(code: PresentCode): Option[Present]

  def commentPresent(presentId: PresentId, comment: String): Unit

  def findByAdminCriteria(params: PresentAdminSearchParams): Seq[Present]

  def countByAdminCriteria(params: PresentAdminSearchParams): Long

  def markAsRemoved(presentId: PresentId, userId: UserId) : UpdateResult

  def updateMediaStorageToS3(presentId: PresentId, newMedia: MediaObjectId): Unit

  def updateMediaStorageToCDN(presentId: PresentId): Unit

  def getUserSentPresents(userId: UserId, request: SentPresentsRequest, offsetLimit: OffsetLimit): Seq[Present]

  def getUserSentPresentsCount(userId: UserId, request: SentPresentsRequest): Long

  def findUserPresents(userId: UserId, productId: ProductId): Seq[Present]

  def findByOrderIds(orderIds: Seq[BigInteger]): Seq[Present]

  def updatePresentStatusForExpiredPresents(): Int

  def assignUserPresents(phone: Option[PhoneShort], email: Option[String])(userId: UserId): Int
}

case class PresentAdminSearchParams(productName: Option[String], locationId: LocationId, status: Option[PresentStatusId], sort: Option[PresentAdminSearchSortType], offset: OffsetLimit)

@Service
class PresentDataServiceImpl extends PresentDataService {
  @Autowired var repo: PresentRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[Present]

  def save(entity: Present) = repo.save(entity)

  def find(id: PresentId): Option[Present] = Option(repo.findOne(id.id))

  def find(ids: Set[PresentId]) = repo.findPresents(ids.map(_.id).toSeq).asScala

  def getUserPresents(userId: UserId, parameters: GetPresentParameters, offsetLimit: OffsetLimit) = {
    val query = buildQueryFromParameters(userId, parameters).withPaging(offsetLimit).desc("t")
    mongo.find(query, entityClass).asScala
  }

  def userPresentsCount(userId: UserId, parameters: GetPresentParameters) = {
    val query = buildQueryFromParameters(userId, parameters)
    mongo.count(query, entityClass)
  }

  def buildQueryFromParameters(userId: UserId, parameters: GetPresentParameters): Query =
    Query.query(new Criteria("uid").is(userId.id)
      .and("hid").nin(userId.id)
      .extend(parameters.status)(status => _.and("cs").is(status))
      .extend(parameters.presentType)(presentType => _.and("p.pt").is(presentType)))

  def activatePresent(id: PresentId): UpdateResult = {
    val update = (new Update).set("rt", DateHelper.currentDate)
      .set("cs", PresentStatusId.received.id)
    mongo.updateFirst(
      Query.query(new Criteria("_id").is(id.id)),
      update,
      entityClass
    ).asUpdateResult
  }

  def findByCode(code: PresentCode) = {
    Option(mongo.findOne(
      Query.query(new Criteria("c").is(code.value)),
      entityClass
    ))
  }

  def commentPresent(presentId: PresentId, comment: String) {
    mongo.updateFirst(
      Query.query(new Criteria("_id").is(presentId.id)),
      (new Update).set("ac", comment),
      entityClass
    )
  }

  implicit private def presentAdminSearchParams2Criteria(params: PresentAdminSearchParams): Criteria =
    Criteria.where("p.lid").is(params.locationId.id)
      .extend(params.productName)(name => _.and("p.pn").regex(s"^${name}", "i"))
      .extend(params.status)(status => _.and("cs").is(status.id))

  def countByAdminCriteria(params: PresentAdminSearchParams) =
    mongo.count(Query.query(params), entityClass)

  def findByAdminCriteria(params: PresentAdminSearchParams) = {
    val query = Query.query(params)
    val sorting = params.sort.getOrElse(PresentAdminSearchSortType.Default).sort
    query.`with`(sorting)
    query.withPaging(params.offset)

    mongo.find(query, entityClass).asScala
  }

  def markAsRemoved(presentId: PresentId, userId: UserId) = {
    val query = Query.query(Criteria.where("_id").is(presentId.id))
    val update = new Update().addToSet("hid", userId.id)
    mongo.updateFirst(query, update, entityClass).asUpdateResult
  }

  def updateMediaStorageToS3(presentId: PresentId, newMedia: MediaObjectId) {
    val query = Query.query(new Criteria("_id").is(presentId.id))
    val update = new Update().set("p.pm.u.t", UrlType.s3.id).set("p.pm.u.mu", newMedia.id)
    mongo.updateFirst(query, update, entityClass)
  }

  def updateMediaStorageToCDN(presentId: PresentId) {
    val query = Query.query(new Criteria("_id").is(presentId.id))
    val update = new Update().unset("p.pm.u.t")
    mongo.updateFirst(query, update, entityClass)
  }

  private[this] def sentPresentsQuery(userId: UserId, request: SentPresentsRequest): Query =
    Query.query(new Criteria("sid").is(userId.id)
      .and("hid").nin(userId)
      .extend(request.presentStatus)(status => _.and("cs").is(status.id))
      .extend(request.presentType)(presentType => _.and("p.pt").is(presentType.id)))

  def getUserSentPresents(userId: UserId, request: SentPresentsRequest, offsetLimit: OffsetLimit) =
    mongo.find(sentPresentsQuery(userId, request).withPaging(offsetLimit).desc("t"), entityClass).asScala

  def getUserSentPresentsCount(userId: UserId, request: SentPresentsRequest) =
    mongo.count(sentPresentsQuery(userId, request), entityClass)

  def findUserPresents(userId: UserId, productId: ProductId) =
    repo.findUserPresentsByProduct(userId.id, productId.id).asScala

  def findByOrderIds(orderIds: Seq[BigInteger]) =
    mongo.find(Query.query(Criteria.where("fid").in(orderIds.asJavaCollection)), entityClass).asScala

  def updatePresentStatusForExpiredPresents() = {
    val query = Query.query(Criteria.where("et").lte(DateHelper.currentDate).and("cs").exists(false))
    val update = new Update().set("cs", PresentStatusId.expired.id)
    mongo.updateMulti(query, update, entityClass).getN
  }

  def assignUserPresents(phone: Option[PhoneShort], email: Option[String])(userId: UserId): Int = {
    import DBCommonConverters._
    val emailCriteria = email map { email => new Criteria("ar.e").is(email) }
    val phoneCriteria = phone map { phone => new Criteria("ar.p").is(phoneShortConverter.serialize(phone)) }

    val criteria = new Criteria("uid").exists(false).orOperator(Seq(emailCriteria, phoneCriteria).flatten: _*)
    val update = new Update().unset("ar").set("uid", userId.id)

    if (phone.isEmpty && email.isEmpty) 0
    else mongo.updateMulti(Query.query(criteria), update, entityClass).getN
  }
}