package com.tooe.core.service

import com.tooe.core.domain.{AdminUserId, CompanyId, SessionToken}
import com.tooe.core.db.mongo.domain.CacheAdminSession
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.CacheAdminSessionRepository
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}

trait CacheAdminSessionDataService {
  def find(token: SessionToken): Option[CacheAdminSession]
  def save(cashSession: CacheAdminSession): CacheAdminSession
  def delete(token: SessionToken): Unit
  def addCompany(userId: AdminUserId, companyId: CompanyId): Unit
}

@Service
class CacheAdminSessionDataServiceImpl extends CacheAdminSessionDataService{
  @Autowired var repository: CacheAdminSessionRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[CacheAdminSession]

  def find(token: SessionToken) = Option(repository.findOne(token.hash))

  def save(cashSession: CacheAdminSession) = repository.save(cashSession)

  def delete(token: SessionToken) = repository.delete(token.hash)

  def addCompany(userId: AdminUserId, companyId: CompanyId) = {
    val query = Query.query(Criteria.where("uid").is(userId.id))
    val update = new Update().addToSet("cids", companyId.id)
    mongo.updateMulti(query, update, entityClass)
  }
}
