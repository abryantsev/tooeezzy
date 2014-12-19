package com.tooe.core.service


import com.tooe.core.db.mongo.domain.OnlineStatus
import com.tooe.core.domain.OnlineStatusId
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Query, Criteria}
import scala.collection.JavaConverters._
import org.springframework.data.domain.Sort

trait OnlineStatusDataService {

  def getStatuses(statusIds: Seq[OnlineStatusId]): Seq[OnlineStatus]

  def save(entity: OnlineStatus): Unit

  def find(onlineStatusId: OnlineStatusId): Option[OnlineStatus]

  def findAll: Seq[OnlineStatus]
}

@Service
class OnlineStatusDataServiceImpl extends OnlineStatusDataService {

  import com.tooe.core.db.mongo.query._

  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[OnlineStatus]

  def save(entity: OnlineStatus) = mongo.save(entity)

  def find(onlineStatusId: OnlineStatusId) = {
    val query = new Query(new Criteria("_id").is(onlineStatusId.id))
    query.`with`(new Sort(Sort.Direction.ASC, "cs"))
    Option(mongo.findOne(new Query(new Criteria("_id").is(onlineStatusId.id)), entityClass))
  }

  def getStatuses(statusIds: Seq[OnlineStatusId]): Seq[OnlineStatus] = {
    val query = new Query(new Criteria("_id").in(statusIds.map(_.id).asJavaCollection))
    query.asc("cs")
    mongo.find(query, entityClass).asScala.toSeq
  }

  def findAll = {
    import scala.collection.JavaConversions._
    val query = new Query()
    query.asc("cs")
    mongo.find(query, entityClass)
  }
}
