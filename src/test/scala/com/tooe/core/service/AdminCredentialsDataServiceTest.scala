package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.db.mongo.domain.AdminCredentials
import com.tooe.core.domain.AdminUserId
import java.util.UUID
import com.tooe.api.service.AdminUserChangeRequest
import com.tooe.core.util.HashHelper._
import com.tooe.core.domain.Unsetable.Skip
import com.tooe.core.db.mongo.query.UpdateResult
import scala.util.Try
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Criteria, Query}

class AdminCredentialsDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: AdminCredentialsDataService = _
  lazy val entities = new MongoDaoHelper("adm_credentials")

  @Test
  def saveAndRead {
    val entity = new AdminCredentialsFixture().adminCredentials
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def representation {
    val entity = new AdminCredentialsFixture().adminCredentials
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println("repr=" + repr)
    jsonAssert(repr)( s"""{
      "_id" : { "$$oid" : "${entity.id.id}" } ,
      "uid" : ${entity.adminUserId.id.mongoRepr},
      "un" : "${entity.userName}",
      "pwd" : "${entity.password}",
      "lpwd" : "${entity.legacyPassword.getOrElse("")}"
    }""")
  }

  @Test
  def find {
    val entity = new AdminCredentialsFixture().adminCredentials
    service.save(entity)

    service.find(entity.userName, entity.password) === Some(entity)
    service.find(entity.userName, "incorrect_password") === None
    service.find("incorrect_username", entity.password) === None
  }

  @Test
  def preventSameLogin {
    val entity = new AdminCredentialsFixture().adminCredentials
    service.save(entity)
    service.save(entity)

    val mongo = service.asInstanceOf[{def mongo: MongoTemplate}].mongo
    mongo.find(Query.query(new Criteria("un").is(entity.userName)), classOf[AdminCredentials]).size() === 1

    service.deleteByUserId(entity.adminUserId)
    service.findByLogin(entity.userName) === None
  }

  @Test
  def change {
    val entity = new AdminCredentialsFixture().adminCredentials
    service.save(entity)

    val request = AdminUserChangeRequest(None, None, Some("new@email" + UUID.randomUUID().toString), Some("new_password"), None, Skip)
    service.find(request.email.get, passwordHash(request.password.get)) === None
    service.change(entity.adminUserId, request)
    service.find(request.email.get, passwordHash(request.password.get)) === Some(entity.copy(userName = request.email.get, password = passwordHash(request.password.get)))

  }

  @Test
  def findByUserIds {
    val entities = (1 to 3).map { _ =>
      new AdminCredentialsFixture().adminCredentials
    }
    entities.foreach(service.save)
    val foundCredentials = service.findByUserIds(entities.map(_.adminUserId))
    foundCredentials must haveSize(3)
    foundCredentials must haveTheSameElementsAs(entities)
  }

  @Test
  def findByUserId {
    val entity = new AdminCredentialsFixture().adminCredentials

    service.save(entity)
    val foundCredentials = service.findByUserId(entity.adminUserId)
    foundCredentials === Some(entity)
  }

  @Test
  def deleteByUserId {
    val entity = new AdminCredentialsFixture().adminCredentials

    service.save(entity)
    service.deleteByUserId(entity.adminUserId)
    service.findByUserId(entity.adminUserId) === None
  }

  @Test
  def replaceLegacyPassword {
    val entity = new AdminCredentialsFixture().adminCredentials.copy(legacyPassword = Some("legacypwdhash"), password = "whatever")
    service.save(entity)

    service.replaceLegacyPassword(entity.userName, "newpwdhash") === UpdateResult.Updated

    val found = service.findOne(entity.id).get
    found.legacyPassword === None
    found.password === "newpwdhash"
  }

  @Test
  def findByLogin {
    val entity = new AdminCredentialsFixture().adminCredentials
    service.save(entity)

    service.findByLogin(entity.userName) === Some(entity)
  }

  @Test
  def emailExist {
    val entity = new AdminCredentialsFixture().adminCredentials
    service.save(entity)

    service.emailExist(entity.userName) === true
    service.emailExist(entity.userName * 2) === false
    service.emailExist(entity.userName.drop(1)) === false
  }
}

class AdminCredentialsFixture {

  val adminCredentials = AdminCredentials(
    adminUserId = AdminUserId(),
    userName = "user_name:" + UUID.randomUUID().toString,
    password = "user_password" + UUID.randomUUID().toString,
    legacyPassword = Some("legacy_password")
  )
}