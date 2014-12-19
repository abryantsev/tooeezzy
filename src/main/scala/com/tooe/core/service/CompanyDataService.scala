package com.tooe.core.service

import com.tooe.core.domain._
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.CompanyRepository
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.api.service.OffsetLimit
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import com.tooe.core.db.mongo.query._
import com.tooe.core.db.mongo.converters.{CompanyMediaConverter, PaymentLimitConverter}
import java.util.Date
import java.util.regex.Pattern
import com.tooe.core.db.mongo.domain.{CompanyIdProjection, Company}
import com.tooe.api.service.SearchCompanyRequest

trait CompanyDataService {
  def find(id: CompanyId): Option[Company]
  def findAllByIds(ids: Seq[CompanyId]): Seq[Company]
  def save(entity: Company): Company

  def getExportedCompanies(): Seq[Company]
  //TODO hasn't test because query very common
  def exportedCompaniesCount(): Long
  def exportedCompaniesComplete(ids: Seq[CompanyId]): Unit

  def search(request: SearchCompanyRequest, offsetLimit: OffsetLimit): Seq[Company]
  def searchCount(request: SearchCompanyRequest): Long

  def getCompanyMedia(id: CompanyId): Option[Seq[MediaObjectId]]

  def updateMediaStorageToS3(companyId: CompanyId, media: MediaObjectId, newMedia: MediaObjectId): Unit
  def updateMediaStorageToCDN(companyId: CompanyId, media: MediaObjectId): Unit

  def findCompaniesByAgentUserId(agentId: AdminUserId): Set[CompanyIdProjection]
}

@Service
class CompanyDataServiceImpl extends CompanyDataService with PaymentLimitConverter with CompanyMediaConverter {

  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._

  @Autowired var repo: CompanyRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[Company]

  val sortMap = Map(
    CompanySort.CompanyName -> "n",
    CompanySort.ContractNumber -> "co.nu",
    CompanySort.ContractDate -> "co.t",
    CompanySort.ModerationStatus -> "mod.s"
  )

  def find(id: CompanyId) = Option(repo.findOne(id.id))

  def findAllByIds(ids: Seq[CompanyId]) = repo.findAll(ids.map(_.id)).asScala.toSeq

  def save(entity: Company) = repo.save(entity)

   //TODO refactoring
  private[this] def exportedCompanyQuery(): Query = {
    Query.query(new Criteria("st").exists(false))
  }


  def getExportedCompanies() = {
    mongo.find(exportedCompanyQuery().withPaging(OffsetLimit(0, 100)).asc("ut"), entityClass).asScala
  }

  def exportedCompaniesCount() = {
     mongo.count(exportedCompanyQuery(), entityClass)
  }

  def exportedCompaniesComplete(ids: Seq[CompanyId]) {
    val query = Query.query(new Criteria("id").in(ids.map(_.id).asJavaCollection))
    val update = new Update().set("st", new Date)
    mongo.updateMulti(query, update, entityClass)
  }

  private[this] def searchCompanyQuery(request: SearchCompanyRequest): Query = {
    import com.tooe.core.util.BuilderHelper._
    Query.query(new Criteria().extend(request.name)(name => _.and("n").regex(Pattern.compile(name))))
  }

  def search(request: SearchCompanyRequest, offsetLimit: OffsetLimit) = {
    val sortField = request.sort.getOrElse(CompanySort.Default)

    val query = searchCompanyQuery(request).asc(sortMap(sortField)).withPaging(offsetLimit)
    mongo.find(query, entityClass).asScala
  }
  def searchCount(request: SearchCompanyRequest) = mongo.count(searchCompanyQuery(request), entityClass)

  def updateMediaStorageToS3(companyId: CompanyId, media: MediaObjectId, newMedia: MediaObjectId) {
    val query = Query.query(new Criteria("_id").is(companyId.id).and("cm.u.mu").is(media.id))
    val update = new Update().set("cm.$.u.t", UrlType.s3.id).set("cm.$.u.mu", newMedia.id)
    mongo.updateFirst(query, update, entityClass)
  }

  def updateMediaStorageToCDN(companyId: CompanyId, media: MediaObjectId) {
    val query = Query.query(new Criteria("_id").is(companyId.id).and("cm.u.mu").is(media.id))
    val update = new Update().unset("cm.$.u.t")
    mongo.updateFirst(query, update, entityClass)
  }

  def findCompaniesByAgentUserId(agentId: AdminUserId) = repo.findCompaniesByAgentUserId(agentId.id).asScala.toSet

  def getCompanyMedia(id: CompanyId) = {
    import com.tooe.core.util.ProjectionHelper._
    val query = Query.query(new Criteria("_id").is(id.id)).extendProjection(Set("cm"))
    Option(mongo.findOne(query, entityClass)).map(_.companyMedia.map(_.url.url))
  }
}
