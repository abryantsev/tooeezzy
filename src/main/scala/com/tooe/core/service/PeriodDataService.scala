package com.tooe.core.service

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.PeriodRepository
import com.tooe.core.db.mongo.domain.Period
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import com.tooe.core.db.mongo.query._

trait PeriodDataService {

  def findAll: Seq[Period]

  def save(p: Period): Period

}

@Service
class PeriodDataServiceImpl extends PeriodDataService {

  import scala.collection.JavaConversions._

  @Autowired var repo: PeriodRepository = _
  @Autowired var mongo: MongoTemplate = _

  def findAll = {
    val query = new Query()
    query.asc("cs")
    mongo.find(query, classOf[Period])
  }

  def save(p: Period) = repo.save(p)
}
