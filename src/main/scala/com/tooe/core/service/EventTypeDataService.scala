package com.tooe.core.service

import com.tooe.core.db.mongo.domain.{UserEvent, EventType}
import com.tooe.core.db.mongo.repository.EventTypeRepository
import com.tooe.core.domain.EventTypeId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Criteria, Query}
import org.springframework.stereotype.Service
import scala.collection.JavaConverters._

trait EventTypeDataService {

  def save(entity: EventType): EventType

  def findOne(id: EventTypeId): Option[EventType]

  def getEventTypes(ids: Seq[EventTypeId]): Seq[EventType]

}

@Service
class EventTypeDataServiceImpl extends  EventTypeDataService {

  @Autowired var repo: EventTypeRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[EventType]

  def save(entity: EventType) = repo.save(entity)

  def findOne(id: EventTypeId) = Option(repo.findOne(id.id))

  def getEventTypes(ids: Seq[EventTypeId]): Seq[EventType] =
    mongo.find(Query.query(new Criteria("id").in(ids.map(_.id).asJavaCollection)), entityClass).asScala.toSeq
}
