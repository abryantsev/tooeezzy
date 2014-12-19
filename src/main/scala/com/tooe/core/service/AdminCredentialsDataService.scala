package com.tooe.core.service

import com.tooe.core.db.mongo.domain.AdminCredentials
import com.tooe.core.domain.{AdminUserId, AdminCredentialsId}
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.AdminCredentialsRepository
import com.tooe.api.service.AdminUserChangeRequest
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import com.tooe.core.util.BuilderHelper
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.util.HashHelper._
import scala.collection.JavaConverters._
import com.tooe.core.db.mongo.query._

trait AdminCredentialsDataService {

  def save(adminUser: AdminCredentials): AdminCredentials

  def findOne(id: AdminCredentialsId): Option[AdminCredentials]

  def find(username: String, passwordHash: String): Option[AdminCredentials]

  def change(adminUserId: AdminUserId, request: AdminUserChangeRequest): Unit

  def findByUserIds(adminUserIds: Seq[AdminUserId]): Seq[AdminCredentials]

  def findByUserId(adminUserId: AdminUserId): Option[AdminCredentials]

  def deleteByUserId(adminUserId: AdminUserId): Unit

  def replaceLegacyPassword(login: String, newPwdHash: String): UpdateResult

  def findByLogin(login: String): Option[AdminCredentials]

  def emailExist(email: String): Boolean
}

@Service
class AdminCredentialsDataServiceImpl extends AdminCredentialsDataService {
  @Autowired var repo: AdminCredentialsRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[AdminCredentials]

  def save(adminCredentials: AdminCredentials) = repo.save(adminCredentials)

  def findOne(id: AdminCredentialsId): Option[AdminCredentials] = Option(repo.findOne(id.id))

  def find(username: String, passwordHash: String) = Option(repo.getCredentials(username, passwordHash))

  def change(adminUserId: AdminUserId, request: AdminUserChangeRequest) {
    import BuilderHelper._

    val query = Query.query(new Criteria("uid").is(adminUserId.id))
    val update = new Update().extend(request.email)(email => _.set("un", email))
                              .extend(request.password)(password => _.set("pwd", passwordHash(password)))
    if(update.nonEmpty)
      mongo.updateFirst(query, update, entityClass)
  }

  def findByUserIds(adminUserIds: Seq[AdminUserId]) = {
    val query = Query.query(new Criteria("uid").in(adminUserIds.map(_.id).asJavaCollection))
    mongo.find(query, entityClass).asScala
  }

  def findByUserId(adminUserId: AdminUserId) = {
    val query = Query.query(new Criteria("uid").is(adminUserId.id))
    Option(mongo.findOne(query, entityClass))
  }

  def deleteByUserId(adminUserId: AdminUserId) {
    val query = Query.query(new Criteria("uid").is(adminUserId.id))
    mongo.remove(query, entityClass)
  }

  def replaceLegacyPassword(login: String, newPwdHash: String) = {
    val query = Query.query(new Criteria("un").is(login))
    val update = new Update().unset("lpwd").set("pwd", newPwdHash)
    val result = mongo.updateFirst(query, update, entityClass)
    result.asUpdateResult
  }

  def findByLogin(login: String) = Option(repo.findByLogin(login))

  def emailExist(email: String) = {
    val query = Query.query(new Criteria("un").is(email))
    mongo.exists(query, entityClass)
  }


}