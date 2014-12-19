package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain.Credentials
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.util.HashHelper
import org.bson.types.ObjectId
import com.tooe.core.util.HashHelper._
import java.util.Date
import com.tooe.api.service.PasswordChangeRequest
import com.tooe.core.db.mongo.query.UpdateResult

class CredentialsDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: CredentialsDataServiceImpl = _

  lazy val entities = new MongoDaoHelper("credentials")

  import HashHelper.uuid
  import com.tooe.core.util.SomeWrapper._

  @Test
  def saveAndReadDelete {
    val f = new CredentialsFixture()
    import f._
    service.findOne(credentials.credentialsId) === None
    service.save(credentials) === credentials
    service.findOne(credentials.credentialsId) === Some(credentials)
    service.delete(credentials.credentialsId)
    service.findOne(credentials.credentialsId) === None
  }

  @Test
  def representation {
    val f = new CredentialsFixture()
    import f._
    service.save(credentials)
    val repr = entities.findOne(credentials.id)
    println("repr="+repr)
    jsonAssert(repr)(s"""{
      "_id" : { "$$oid" : "${credentials.id}"} ,
      "_class" : "com.tooe.core.db.mongo.domain.Credentials" ,
      "uid" : { "$$oid" : "${credentials.uid}"} ,
      "un" : "${credentials.userName}" ,
      "pwd" : "${credentials.passwordHash}" ,
      "lpwd" : "${credentials.legacyPasswordHash.get}" ,
      "fid" : "${credentials.facebookId.get}" ,
      "vid" : "${credentials.vkontakteId.get}" ,
      "tid" : "${credentials.twitterId.get}" ,
      "vt" : ${credentials.verificationTime.get.mongoRepr} ,
      "vk" : "${credentials.verificationKey.get}"
    }""")
  }

  @Test
  def find {
    val f = new CredentialsFixture()
    import f._
    def find = service.find(userName = credentials.userName, passwordHash = credentials.passwordHash)
    find === None
    service.save(credentials)
    find === Some(credentials)
  }

  @Test
  def findByVerificationKey {
    val f = new CredentialsFixture()
    import f._
    val verifiedCredentials = credentials.copy(verificationKey = uuid)
    def find = service.find(verifiedCredentials.getVerificationKey.get)
    find === None
    service.save(verifiedCredentials)
    find === Some(verifiedCredentials)
  }

  @Test
  def updatePassword {

    val f = new CredentialsFixture()
    import f._
    service.save(credentials)
    val newPassword = "new password"
    val newPasswordHash = HashHelper.passwordHash(newPassword)
    val request = PasswordChangeRequest(password, newPassword, newPassword)
    val find = () => service.find(userName = credentials.userName, passwordHash = newPasswordHash)
    find() === None
    service.updateUserPassword(credentials.userId, request)
    find() === Some(credentials.copy(passwordHash = newPasswordHash))
  }

  @Test
  def replaceLegacyPassword {
    val entity = new CredentialsFixture().credentials.copy(legacyPasswordHash = Some("legacypwdhash"), passwordHash = "whatever")
    service.save(entity)

    service.replaceLegacyPassword(entity.userName, "newpwdhash") === UpdateResult.Updated

    val found = service.findOne(entity.credentialsId).get
    found.legacyPasswordHash === None
    found.passwordHash === "newpwdhash"
  }

  @Test
  def changePassword {
    val entity = new CredentialsFixture().credentials
    service.save(entity)

    val newPasswordHash = HashHelper.passwordHash("newPassword")
    service.changePassword(entity.userName, newPasswordHash)

    val found = service.findOne(entity.credentialsId).get
    found.passwordHash === newPasswordHash
  }

  @Test
  def findByLogin {
    val entity = new CredentialsFixture().credentials
    service.save(entity)

    service.findByLogin(entity.userName) === Some(entity)
  }

  @Test
  def findByUserIds {
    val entity = new CredentialsFixture().credentials
    service.save(entity)

    service.findByUserIds(Seq(entity.userId)) === Seq(entity)
  }
}

class CredentialsFixture {

  import com.tooe.core.util.SomeWrapper._

  val password = "password"

  val credentials = Credentials(
    uid = new ObjectId,
    userName = uuid,
    passwordHash = HashHelper.passwordHash(password),
    legacyPasswordHash = HashHelper.sha1("legacyPasswordHash"),
    facebookId = "facebookId",
    vkontakteId = "vkontakteId",
    twitterId = "twitterId",
    verificationTime = new Date,
    verificationKey = "verificationKey"
  )
}