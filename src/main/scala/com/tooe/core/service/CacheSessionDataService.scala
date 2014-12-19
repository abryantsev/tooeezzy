package com.tooe.core.service

import com.tooe.core.db.mongo.domain.CacheSession
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.domain.UserId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Criteria
import com.tooe.core.db.mongo.repository.CacheSessionRepository
import com.tooe.core.db.mongo.domain.User
import com.tooe.core.db.mongo.domain.CacheSession
import com.tooe.core.util.HashHelper
import com.tooe.core.domain.SessionToken
import com.tooe.core.util.DateHelper

trait CacheSessionDataService {
  def find(token: SessionToken): Option[CacheSession]
  def save(cashSession: CacheSession): CacheSession
  def delete(token: SessionToken): Unit
}

@Service
class CacheSessionDataServiceImpl extends CacheSessionDataService {
  @Autowired var repository: CacheSessionRepository = _

  def find(token: SessionToken) = Option(repository.findOne(token.hash))
  
  def save(cashSession: CacheSession) = repository.save(cashSession)
  
  def delete(token: SessionToken) = repository.delete(token.hash)

  def entityClass = classOf[CacheSession]
}