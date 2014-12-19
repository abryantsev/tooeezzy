package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.db.mongo.domain.{CalendarDates, CalendarEvent}
import org.junit.Test
import com.tooe.core.domain.UserId
import java.util.Date
import com.tooe.core.domain.Unsetable.Update
import com.tooe.core.usecase.calendarevent.{SearchCalendarEventsRequest, CalendarEventChangeRequest}
import com.tooe.core.util.DateHelper
import com.tooe.api.service.OffsetLimit

class CalendarEventDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: CalendarEventDataService = _

  lazy val entities = new MongoDaoHelper("calendarevents")

  @Test
  def readWrite() {
    val entity = new CalendarEventFixture().event
    service.find(entity.id) === None
    service.save(entity)
    service.find(entity.id) === Some(entity)
  }

  @Test
  def representation {
    val entity = new CalendarEventFixture().event
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    jsonAssert(repr)(s"""{
      "_id" : ${entity.id.id.mongoRepr} ,
      "uid" : ${entity.userId.id.mongoRepr},
      "n"   : "${entity.name}",
      "d"   : "${entity.description.getOrElse("")}",
      "ds"  : {
        "d" : ${entity.dates.date.mongoRepr},
        "t" : ${entity.dates.time.getOrElse(new Date(0)).mongoRepr}
      }
    }""")
  }

  @Test
  def update {
    val entity = new CalendarEventFixture().event
    service.save(entity)

    val expectedEvent = CalendarEvent(id = entity.id, userId = entity.userId, name = "new name", description = Some("new description"), dates = CalendarDates(new Date, Some(new Date)))

    val request = CalendarEventChangeRequest(name = Some(expectedEvent.name),
      description = Update(expectedEvent.description.get),
      date = Some(expectedEvent.dates.date),
      time = Update(expectedEvent.dates.time.get))

    service.update(entity.id, request)

    service.find(entity.id) === Some(expectedEvent)
  }

  @Test
  def searchEvents {
    import DateHelper._
    val entity = new CalendarEventFixture().event
    service.save(entity)

    val requestByName = SearchCalendarEventsRequest(
      name = Some(entity.name.take(3)),
      offsetLimit = OffsetLimit(0, 15)
    )
    val requestByDate = SearchCalendarEventsRequest(
      startTime = Some(entity.dates.date.addMillis(-10000)),
      endTime = Some(entity.dates.date.addDays(7)),
      offsetLimit = OffsetLimit(0, 15)
    )

    service.searchEvents(entity.userId, requestByName) === Seq(entity)
    service.searchEvents(entity.userId, requestByDate) === Seq(entity)
  }
}

class CalendarEventFixture {

  private val eventDates = CalendarDates(new Date, Some(new Date))

  val event = CalendarEvent(
    userId = UserId(),
    name = "some name",
    description = Some("description"),
    dates = eventDates
  )

}
