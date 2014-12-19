package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.domain.{AdminUserId, LifecycleStatusId, CompanyId, AdminRoleId}
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.api.service.{OffsetLimit, SearchAdminUserRequest, AdminUserChangeRequest}
import java.util.UUID
import com.tooe.core.db.mongo.domain.AdminUser
import com.tooe.core.domain.Unsetable.Update

class AdminUserDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: AdminUserDataService = _
  lazy val entities = new MongoDaoHelper("adm_user")

  @Test
  def saveAndRead {
    val entity = new AdminUserFixture().adminUser
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def representation {
    val entity = new AdminUserFixture().adminUser
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println("repr=" + repr)
    jsonAssert(repr)( s"""{
      "_id" : { "$$oid" : "${entity.id.id}" } ,
      "n" : "${entity.name}",
      "ln" : "${entity.lastName}",
      "ns" : [ ${entity.names.mkString("\"","\",\"","\"")} ] ,
      "rt" : ${entity.registrationDate.mongoRepr},
      "r" : "${entity.role.id}",
       "cid" : { "$$oid" : "${entity.companyId.get.id.toString}" },
       "d" : "${entity.description.getOrElse("")}",
       "lfs" : "${entity.lifecycleStatus.get.id}"
    }""")
  }

  @Test
  def change {
    val entity = new AdminUserFixture().adminUser
    service.save(entity)

    val request = AdminUserChangeRequest(Some("new name"), Some("new last name"), None, None, Some(AdminRoleId.Superagent), Update("new description"))
    service.change(entity.id, request)
    service.findOne(entity.id) === Some(entity.copy(name = request.name.get, lastName = request.lastName.get, role = request.role.get, description = Some(request.description.get)))
  }

  @Test
  def search {
    val archivedEntity = new AdminUserFixture().adminUser
    val entity = archivedEntity.copy(id = AdminUserId(), lifecycleStatus = None)
    service.save(entity)

    val request = SearchAdminUserRequest(name = Some(entity.name), sort = None)
    val foundEntities = service.search(request, OffsetLimit(0, 10))

    foundEntities must haveSize(1)
    foundEntities.head === entity

    val requestWithHalfName = SearchAdminUserRequest(name = Some(entity.name.take(entity.name.length / 2).toUpperCase), sort = None)
    val foundEntitiesWithHalfName = service.search(requestWithHalfName, OffsetLimit())

    foundEntitiesWithHalfName must contain(entity)
    foundEntitiesWithHalfName must not contain (archivedEntity)

    val requestWithHalfLastName = SearchAdminUserRequest(name = Some(entity.lastName.take(entity.lastName.length / 2).toUpperCase), sort = None)
    val foundEntitiesWithHalfLastName = service.search(requestWithHalfLastName, OffsetLimit())

    foundEntitiesWithHalfLastName must contain(entity)
    foundEntitiesWithHalfLastName must not contain (archivedEntity)
  }

  @Test
  def getRole() {
    val entity = new AdminUserFixture().adminUser
    service.save(entity)
    service.getRole(entity.id) === entity.role
  }

  @Test
  def count() {
    val entity = new AdminUserFixture().adminUser.copy(lifecycleStatus = None)
    service.save(entity)
    val request = SearchAdminUserRequest(name = Some(entity.name), sort = None)
    service.count(request) === 1
  }

  @Test
  def activateUser {
    val entity = new AdminUserFixture().adminUser.copy(lifecycleStatus = Some(LifecycleStatusId.Deactivated))
    service.save(entity)

    val companyId = CompanyId()

    service.activateUser(entity.id, companyId)

    val activeUser = service.findOne(entity.id)
    activeUser.flatMap(_.lifecycleStatus) === None
    activeUser.flatMap(_.companyId) === Some(companyId)
  }

  @Test
  def delete {
    val entity = new AdminUserFixture().adminUser
    service.save(entity)

    service.delete(entity.id)

    service.findOne(entity.id) === Some(entity.copy(lifecycleStatus = Some(LifecycleStatusId.Removed)))
  }

  @Test
  def find {
    val entity = new AdminUserFixture().adminUser
    service.find(Seq(entity.id)) === Seq()
    service.save(entity)
    service.find(Seq(entity.id)) === Seq(entity)
  }

  @Test
  def findActiveUser {

    val inactive = new AdminUserFixture().adminUser
    service.save(inactive)

    service.findActiveUser(inactive.id) === None

    val active = new AdminUserFixture().adminUser.copy(lifecycleStatus = None)
    service.save(active)

    service.findActiveUser(active.id) === Some(active)

  }

  @Test
  def findByRolesForCompany() {
    def getRole(i: Int) = if (i % 2 == 0) AdminRoleId.Client else AdminRoleId.Agent
    val companyId = CompanyId()
    val users = (1 to 5).map(i => service.save(new AdminUserFixture().adminUser.copy(role = getRole(i), companyId = Some(companyId)))).toSeq

    val result = service.findByRolesForCompany(companyId, Seq(AdminRoleId.Client, AdminRoleId.Agent))
    result.size === users.size
    result.zip(users).foreach {
      case (f, e) => f === e
    }
  }

  @Test
  def findAdminUsersByCompanyAndRoles {

    val admin1 = new AdminUserFixture().adminUser.copy(role = AdminRoleId.Dealer)
    val admin2 = new AdminUserFixture().adminUser

    service.save(admin1)
    service.save(admin2)

    val foundUsers = service.findAdminUsersByCompanyAndRoles(Seq(admin1.companyId.get, admin2.companyId.get), Seq(admin1.role))

    foundUsers must contain(admin1)
    foundUsers must not contain(admin2)
  }

}

class AdminUserFixture {

  val adminUser = AdminUser(
    name = "name:" + UUID.randomUUID().toString,
    lastName = "last_name:" + UUID.randomUUID().toString,
    role = AdminRoleId.Admin,
    companyId = Some(CompanyId()),
    description = Some("description"),
    lifecycleStatus = Some(LifecycleStatusId.Archived)
  )
}