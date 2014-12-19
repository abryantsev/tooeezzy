package com.tooe.core.service

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.domain.CacheWriteSniffer
import com.tooe.core.domain.{WriteSnifferCacheId, UserId}
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Criteria, Query}
import com.tooe.core.db.mongo.repository.CacheWriteSnifferRepository

trait CacheWriteSnifferDataService {
  def find(id: WriteSnifferCacheId): Option[CacheWriteSniffer]
  def recordsCount(userId: UserId): Int
  def save(entity: CacheWriteSniffer): CacheWriteSniffer
}

@Service
class CacheWriteSnifferDataServiceImpl extends CacheWriteSnifferDataService {

  @Autowired var repo: CacheWriteSnifferRepository = _
  @Autowired var mongo: MongoTemplate = _
  val entityClass = classOf[CacheWriteSniffer]


  def find(id: WriteSnifferCacheId): Option[CacheWriteSniffer] = Option(mongo.findOne(Query.query(new Criteria("_id").is(id.id)), entityClass))

  def recordsCount(userId: UserId) = mongo.count(Query.query(new Criteria("uid").is(userId.id)), entityClass).toInt

  def save(entity: CacheWriteSniffer) = repo.save(entity)
}
