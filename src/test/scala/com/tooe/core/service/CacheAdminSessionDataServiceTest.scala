package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.db.mongo.domain.CacheAdminSession
import com.tooe.core.domain.{CompanyId, AdminRoleId, SessionToken, AdminUserId}
import java.util.{UUID, Date}

class CacheAdminSessionDataServiceTest extends SpringDataMongoTestHelper {

  @Autowired var service: CacheAdminSessionDataService = _
  lazy val entities = new MongoDaoHelper("cache_adm_sessions")

  @Test
  def saveAndRead {
    val entity = new CacheAdminSessionFixture().cacheAdminSession
    service.find(entity.id) === None
    service.save(entity) === entity
    service.find(entity.id) === Some(entity)
    service.delete(entity.id)
    service.find(entity.id) === None
  }

  @Test
  def representation {
    val company = CompanyId()
    val entity = new CacheAdminSessionFixture(companies = Seq(company)).cacheAdminSession
    service.save(entity)
    val repr = entities.findOne(entity.id.hash)
    println("repr=" + repr)
    jsonAssert(repr)( s"""{
      "_id" : "${entity.id.hash}"  ,
      "uid" : ${entity.adminUserId.id.mongoRepr},
      "t" : ${entity.time.mongoRepr},
      "r" : "${entity.role.id}",
      "cids" : [ ${company.id.mongoRepr} ]
    }""")
  }

  @Test
  def addToCompanies {
    val userId = AdminUserId()
    val s1, s2 = new CacheAdminSessionFixture(adminUserId = userId).cacheAdminSession
    val sessions = Seq(s1, s2)
    sessions foreach service.save

    val newCompanyId = CompanyId()
    service.addCompany(userId, newCompanyId)

    val found = sessions map (_.id) flatMap service.find

    found zip sessions foreach { case (f, s) =>
      f.companies.toSet === s.companies.toSet + newCompanyId
    }
  }
}

class CacheAdminSessionFixture(adminUserId: AdminUserId = AdminUserId(), companies: Seq[CompanyId] = Seq(CompanyId())) {

  val cacheAdminSession = CacheAdminSession(
    id = SessionToken("token:" + UUID.randomUUID().toString),
    time = new Date,
    adminUserId = adminUserId,
    role = AdminRoleId.Superagent,
    companies = companies
  )
}
