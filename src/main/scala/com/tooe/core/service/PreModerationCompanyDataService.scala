package com.tooe.core.service

import com.tooe.core.domain._
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.PreModerationCompanyRepository
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.db.mongo.converters.{DBObjectConverters, PreModerationStatusConverter, CompanyMediaConverter, PaymentLimitConverter}
import com.tooe.api.service._
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import java.util.Date
import com.tooe.core.db.mongo.query._
import java.util.regex.Pattern
import scala.collection.JavaConverters._
import com.tooe.core.util.BuilderHelper
import scala.Some
import com.tooe.core.db.mongo.domain._

trait PreModerationCompanyDataService {
  def find(id: PreModerationCompanyId): Option[PreModerationCompany]
  def save(entity: PreModerationCompany): PreModerationCompany
  def updateStatus(companyId: PreModerationCompanyId, request: CompanyModerationRequest, userId: AdminUserId,  publishId: Option[CompanyId]): Unit
  def getPublishCompanyId(id: PreModerationCompanyId): Option[CompanyId]
  def search(request: SearchModerationCompanyRequest, offsetLimit: OffsetLimit): Seq[PreModerationCompany]
  def searchCount(request: SearchModerationCompanyRequest): Long
  def changeCompany(companyId: PreModerationCompanyId, request: CompanyChangeRequest): UpdateResult
  def findByCompanyId(id: CompanyId): Option[PreModerationCompany]
  def updateCompanyMedia(id: PreModerationCompanyId, media: CompanyMedia): UpdateResult
  def updateMediaStorageToS3(companyId: PreModerationCompanyId, media: MediaObjectId, newMedia: MediaObjectId): Unit
  def updateMediaStorageToCDN(companyId: PreModerationCompanyId, media: MediaObjectId): Unit

}

@Service
class PreModerationCompanyDataServiceImpl extends PreModerationCompanyDataService with PaymentLimitConverter
                                                                                  with CompanyMediaConverter
                                                                                  with PreModerationStatusConverter {
  @Autowired var repo: PreModerationCompanyRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[PreModerationCompany]

  def find(id: PreModerationCompanyId) = Option(repo.findOne(id.id))

  def save(entity: PreModerationCompany) = repo.save(entity)

  def updateStatus(companyId: PreModerationCompanyId, request: CompanyModerationRequest, userId: AdminUserId, publishId: Option[CompanyId]) {
    import DBObjectConverters._

    val query = Query.query(new Criteria("_id").is(companyId.id))
    val update = new Update().setSerialize("mod", PreModerationStatus(request.status, request.message, Some(userId), Some(new Date)))
                             .setOrSkip("puid", publishId)
    mongo.updateFirst(query, update, entityClass)
  }

  def getPublishCompanyId(id: PreModerationCompanyId) = {

    import com.tooe.core.util.ProjectionHelper._

    val query = Query.query(new Criteria("_id").is(id.id)).withPaging(OffsetLimit(0,1)).extendProjection(Set("puid"))
    Option(mongo.findOne(query, entityClass)).flatMap(_.publishCompany)
  }


  val sortMap = Map(
    PreModerationSort.CompanyName -> "n",
    PreModerationSort.ContractNumber -> "co.nu",
    PreModerationSort.ContractDate -> "co.t",
    PreModerationSort.ModerationStatus -> "mod.s"
  )

  def search(request: SearchModerationCompanyRequest, offsetLimit: OffsetLimit) = {
    val sortField = request.sort.getOrElse(PreModerationSort.Default)

    val query = searchCompanyQuery(request).asc(sortMap(sortField)).withPaging(offsetLimit)
    mongo.find(query, entityClass).asScala

  }


  private[this] def searchCompanyQuery(request: SearchModerationCompanyRequest): Query = {
    import com.tooe.core.util.BuilderHelper._
    Query.query(new Criteria().extend(request.name)(name => _.and("n").regex(Pattern.compile(name)))
      .extend(request.status)(status => _.and("mod.s").is(status.id)))
  }

  def searchCount(request: SearchModerationCompanyRequest) = mongo.count(searchCompanyQuery(request), entityClass)

  def changeCompany(companyId: PreModerationCompanyId, request: CompanyChangeRequest) = {
    import BuilderHelper._
    import com.tooe.core.db.mongo.converters.DBObjectConverters._

    val query = Query.query(new Criteria("_id").is(companyId.id))
    val update = new Update().extend(request.agentId)(aid => _.set("aid", aid.id))
      .extend(request.contractDate)(date => _.set("co.t", date))
      .extend(request.contractNumber)(number => _.set("co.nu", number))
      .extend(request.phone)(phone => _.set("c.p.0.n", phone))
      .extend(request.countryCode)(code => _.set("c.p.0.c", code))
      .extend(request.management)(management => _.extend(management.office)(office => _.set("m.on", office))
                      .extend(management.manager)(manager => _.set("m.mn", manager))
                      .extend(management.accountant)(accountant => _.set("m.an", accountant)))
      .extend(request.description)(desc => _.set("d", desc))
      .setSkipUnset("c.url", request.url)
      .extend(request.address)(address => _.set("c.a", address))
      .extend(request.legalInfo)(legalInfo => _.extend(legalInfo.structure)(structure => _.set("l.cs", structure))
                      .setSkipUnset("l.ogrn", legalInfo.ogrn)
                      .setSkipUnset("l.lrt", legalInfo.registrationDate)
                      .setSkipUnset("l.ri", legalInfo.registrator)
                      .extend(legalInfo.taxNumber)(tax => _.set("l.tn", tax))
                      .setSkipUnset("l.kpp", legalInfo.kpp)
                      .setSkipUnset("l.li", legalInfo.licence)
                      .setSkipUnset("su", legalInfo.subject)
                      .extend(legalInfo.legalAddress)(la => _.set("c.la", la)))
      .setOrSkipSeq("cm", request.media.map(media => Seq(CompanyMedia(MediaObject(MediaObjectId(media)), Some("main")))))
      .setSkipUnset("sn", request.shortName)
      .extend(request.companyName)(name => _.set("n", name))
      .extend(request.payment)(payment => _.extend(payment.bankAccount)(ba => _.set("p.ba", ba))
      .extend(payment.bankIndex)(bi => _.set("p.bi", bi))
      .extend(payment.bankTransferAccount)(bta => _.set("p.bt", bta))
      .setSkipUnset("p.p", payment.period)
      .setSkipUnset("p.l", payment.limit.map(l => PaymentLimit(l.value, l.currency))))
      .setSerialize("mod.s", ModerationStatusId.Waiting)


    mongo.updateFirst(query, update, entityClass).asUpdateResult
  }

  def findByCompanyId(id: CompanyId) = Option(mongo.findOne(Query.query(new Criteria("puid").is(id.id)), entityClass))

  def updateMediaStorageToS3(companyId: PreModerationCompanyId, media: MediaObjectId, newMedia: MediaObjectId) {
    val query = Query.query(new Criteria("_id").is(companyId.id).and("cm.u.mu").is(media.id))
    val update = new Update().set("cm.$.u.t", UrlType.s3.id).set("cm.$.u.mu", newMedia.id)
    mongo.updateFirst(query, update, entityClass)
  }

  def updateMediaStorageToCDN(companyId: PreModerationCompanyId, media: MediaObjectId) {
    val query = Query.query(new Criteria("_id").is(companyId.id).and("cm.u.mu").is(media.id))
    val update = new Update().unset("cm.$.u.t")
    mongo.updateFirst(query, update, entityClass)
  }

  def updateCompanyMedia(id: PreModerationCompanyId, media: CompanyMedia) = {
    val query = Query.query(new Criteria("_id").is(id.id))
    val update = new Update().setSerializeSeq("cm", Seq(media))
    mongo.updateFirst(query, update, entityClass).asUpdateResult
  }
}
