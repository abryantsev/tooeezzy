package com.tooe.core.service

import com.tooe.core.db.mongo.converters.DBObjectConverters._
import com.tooe.core.db.mongo.domain.CalendarEvent
import com.tooe.core.db.mongo.query._
import com.tooe.core.db.mongo.repository.CalendarEventRepository
import com.tooe.core.domain.{UserId, CalendarEventId}
import com.tooe.core.usecase.calendarevent.{SearchCalendarEventsRequest, CalendarEventChangeRequest}
import com.tooe.core.util.BuilderHelper._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import org.springframework.stereotype.Service
import java.util.regex.Pattern
import scala.collection.JavaConverters._

trait CalendarEventDataService {

  def find(id: CalendarEventId): Option[CalendarEvent]

  def save(event: CalendarEvent): CalendarEvent

  def update(eventId: CalendarEventId, request: CalendarEventChangeRequest): Unit

  def delete(eventId: CalendarEventId): Unit

  def searchEvents(userId: UserId, request: SearchCalendarEventsRequest): Seq[CalendarEvent]

  def searchEventsCount(userId: UserId, request: SearchCalendarEventsRequest): Long

}

@Service
class CalendarEventDataServiceImpl extends CalendarEventDataService {
  @Autowired var repo: CalendarEventRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[CalendarEvent]

  def find(id: CalendarEventId): Option[CalendarEvent] = Option(repo.findOne(id.id))

  def save(event: CalendarEvent): CalendarEvent = repo.save(event)

  def update(eventId: CalendarEventId, request: CalendarEventChangeRequest) {
    val query = Query.query(new Criteria("_id").is(eventId.id))
    val update = new Update().extend(request.name)(name => _.set("n", name))
                            .setSkipUnset("d", request.description)
                            .extend(request.date)(date => _.set("ds.d", date))
                            .setSkipUnset("ds.t", request.time)
    if(update.nonEmpty)
      mongo.updateFirst(query, update, entityClass)
  }

  def searchEvents(userId: UserId, request: SearchCalendarEventsRequest): Seq[CalendarEvent] = {
    mongo.find(searchEventsQuery(userId, request), entityClass).asScala.toSeq
  }


  private def searchEventsQuery(userId: UserId, request: SearchCalendarEventsRequest) = {
    Query.query(new Criteria("uid").is(userId.id)
      .extend(request.name)(name => _.and("n").regex(Pattern.compile(s"^${name.toLowerCase}")))
      .extend(request.startTime)(startTime => _.and("ds.d").gte(startTime).lte(request.endTime.get))
    )
  }

  def searchEventsCount(userId: UserId, request: SearchCalendarEventsRequest): Long = {
    mongo.count(searchEventsQuery(userId, request), entityClass)
  }

  def delete(eventId: CalendarEventId): Unit = {
    repo.delete(eventId.id)
  }
}
