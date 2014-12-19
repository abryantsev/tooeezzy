package com.tooe.core.service

import com.tooe.core.db.mongo.domain.AdminUser
import com.tooe.core.domain._
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.AdminUserRepository
import com.tooe.api.service.OffsetLimit
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import com.tooe.core.db.mongo.query._
import com.tooe.core.util.BuilderHelper
import org.springframework.data.mongodb.core.MongoTemplate
import java.util.regex.Pattern
import org.springframework.data.domain.Sort
import scala.collection.JavaConverters._
import com.tooe.api.service.AdminUserChangeRequest
import com.tooe.core.domain.AdminUserId
import com.tooe.api.service.SearchAdminUserRequest

trait AdminUserDataService {

  def save(adminUser: AdminUser): AdminUser

  def findOne(id: AdminUserId): Option[AdminUser]

  def find(ids: Seq[AdminUserId]): Seq[AdminUser]

  def findActiveUser(id: AdminUserId): Option[AdminUser]

  def findByRolesForCompany(companyId: CompanyId, roles: Seq[AdminRoleId]): Seq[AdminUser]

  def change(adminUserId: AdminUserId, request: AdminUserChangeRequest): Unit

  def search(request: SearchAdminUserRequest, offsetLimit: OffsetLimit): Seq[AdminUser]

  def count(request: SearchAdminUserRequest): Long

  def activateUser(adminUserId: AdminUserId, companyId: CompanyId): Unit

  def delete(adminUserId: AdminUserId): Unit

  def getRole(adminUserId: AdminUserId): AdminRoleId

  def findAdminUsersByCompanyAndRoles(companyIds: Seq[CompanyId], roles: Seq[AdminRoleId]): Seq[AdminUser]

}

@Service
class AdminUserDataServiceImpl extends AdminUserDataService {
  @Autowired var repo: AdminUserRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[AdminUser]

  val mongoSortFields = Map(
    AdminUserSort.Name -> "n",
    AdminUserSort.LastName -> "ln",
    AdminUserSort.RegDate -> "rt",
    AdminUserSort.Role -> "r"
  )

  val mongoProjectionFields = Map(
    AdminUserField.Name -> "n",
    AdminUserField.LastName -> "ln",
    AdminUserField.RegDate -> "rt",
    AdminUserField.Role -> "r"
  )

  def save(adminUser: AdminUser) = repo.save(adminUser)

  def findOne(id: AdminUserId): Option[AdminUser] = Option(repo.findOne(id.id))

  def change(adminUserId: AdminUserId, request: AdminUserChangeRequest) {
    import BuilderHelper._
    import com.tooe.core.db.mongo.converters.DBObjectConverters._

    val query = Query.query(new Criteria("_id").is(adminUserId.id))
    val update = new Update().extend(request.name)(name => _.set("n", name))
      .extend(request.lastName)(lastname => _.set("ln", lastname))
      .extend(request.role)(role => _.setSerialize("r", role))
      .setSkipUnset("d", request.description)

    if(update.nonEmpty)
      mongo.updateFirst(query, update, entityClass)

    if (!(request.name orElse request.lastName).isEmpty) {
      mongo.save(mongo.findOne(query, entityClass))
    }
  }

  def search(request: SearchAdminUserRequest, offsetLimit: OffsetLimit) = {

    val sortField = request.sort.getOrElse(AdminUserSort.Default)

    val query = Query.query(request)
      .withPaging(offsetLimit)
      .sort(new Sort(mongoSortFields(sortField)))
    query.fields().exclude("ns")
    mongo.find(query, entityClass).asScala
  }

  def count(request: SearchAdminUserRequest) = mongo.count(Query.query(request), entityClass)

  def activateUser(adminUserId: AdminUserId, companyId: CompanyId) {
    val query = Query.query(new Criteria("_id").is(adminUserId.id))
    val update = new Update().unset("lfs").set("cid", companyId.id)
    mongo.updateFirst(query, update, entityClass)
  }

  def delete(adminUserId: AdminUserId) {
    val query = Query.query(new Criteria("_id").is(adminUserId.id))
    val update = new Update().set("lfs", LifecycleStatusId.Removed.id)
    mongo.updateFirst(query, update, entityClass)
  }

  def find(ids: Seq[AdminUserId]) = repo.findAll(ids.map(_.id).asJavaCollection).asScala.toSeq

  def findActiveUser(id: AdminUserId): Option[AdminUser] = {
    val query = Query.query(new Criteria("_id").is(id.id).and("lfs").exists(false))
    query.fields().exclude("ns")
    Option(mongo.findOne(query, entityClass))
  }

  def findByRolesForCompany(companyId: CompanyId, roles: Seq[AdminRoleId]) = {
    val query = Query.query(Criteria.where("cid").is(companyId.id).and("r").in(roles.map(_.id): _*))
    query.fields.exclude("ns")
    mongo.find(query, entityClass).asScala
  }

  def getRole(adminUserId: AdminUserId): AdminRoleId = {
    repo.getRole(adminUserId.id).role
  }

  private implicit def searchAdminUserRequestToCriteria(request: SearchAdminUserRequest): Criteria = {
    import BuilderHelper._
    new Criteria().extend(request.name)(name => _.and("ns").in(Pattern.compile(s"^${name.toLowerCase}")))
      .extend(request.showAll.getOrElse(false))(c => c, _.and("lfs").exists(false) )
  }

  def findAdminUsersByCompanyAndRoles(companyIds: Seq[CompanyId], roles: Seq[AdminRoleId]) = {
    val query = Query.query(Criteria.where("cid").in(companyIds.map(_.id).asJavaCollection).and("r").in(roles.map(_.id).asJavaCollection))
    query.fields.exclude("ns")
    mongo.find(query, entityClass).asScala
  }
}
