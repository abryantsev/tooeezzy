package com.tooe.core.service

import com.tooe.core.db.mongo.domain.Credentials
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.CredentialsRepository
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import com.tooe.core.util.HashHelper._
import com.tooe.core.domain.CredentialsId
import com.tooe.core.domain.UserId
import com.tooe.api.service.PasswordChangeRequest
import com.tooe.core.domain.VerificationKey
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.db.mongo.query._
import scala.collection.JavaConverters._
import com.tooe.core.exceptions.ApplicationException
import spray.http.StatusCodes

trait CredentialsDataService {

  def find(userName: String, passwordHash: String): Option[Credentials]

  def find(verificationKey: VerificationKey): Option[Credentials]

  def delete(credentialId: CredentialsId): Unit

  def updateUserPassword(userId: UserId, request: PasswordChangeRequest): Boolean

  def replaceLegacyPassword(login: String, newPwdHash: String): UpdateResult

  def changePassword(login: String, newPwdHash: String): UpdateResult

  def save(entity: Credentials): Credentials

  def findOne(id: CredentialsId): Option[Credentials]

  def findByLogin(login: String): Option[Credentials]
  
  def findByUserIds(userId: Seq[UserId]): Seq[Credentials]
}

@Service
class CredentialsDataServiceImpl extends CredentialsDataService {
  @Autowired var repo: CredentialsRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[Credentials]

  def find(userName: String, passwordHash: String) = Option(repo.getUserCredentials(userName, passwordHash))

  def find(verificationKey: VerificationKey): Option[Credentials] =
    Option(repo.findUserCredentialsByVerificationKey(verificationKey.value))

  def delete(credentialId: CredentialsId) { repo.delete(credentialId.id)}

  def updateUserPassword(userId: UserId, request: PasswordChangeRequest) = {

    val query = Query.query(new Criteria("uid").is(userId.id).and("pwd").is(passwordHash(request.oldPassword)))
    val update = new Update().set("pwd", passwordHash(request.newPassword))

    mongo.exists(query, entityClass) match {
      case true =>
        mongo.updateFirst(query, update, entityClass)
        true
      case false => throw ApplicationException(400, message = "Wrong password", StatusCodes.BadRequest)
    }


  }

  def replaceLegacyPassword(login: String, newPwdHash: String) = {
    val query = Query.query(new Criteria("un").is(login))
    val update = new Update().unset("lpwd").set("pwd", newPwdHash)
    mongo.updateFirst(query, update, entityClass).asUpdateResult
  }

  def changePassword(login: String, newPwdHash: String) = {
    val query = Query.query(new Criteria("un").is(login))
    val update = new Update().set("pwd", newPwdHash)
    mongo.updateFirst(query, update, entityClass).asUpdateResult
  }

  def save(entity: Credentials) = repo.save(entity)

  def findOne(id: CredentialsId) = Option(repo.findOne(id.id))

  def findByLogin(login: String) = Option(repo.findByLogin(login))
  
  def findByUserIds(userIds: Seq[UserId]) = {
    val query = Query.query(new Criteria("uid").in(userIds.map(_.id).asJavaCollection))
    mongo.find(query, entityClass).asScala.toSeq
  }
}