package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.db.mongo.domain.AdminUserEvent
import com.tooe.core.domain.AdminUserId
import org.junit.Test

class AdminUserEventDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service:  AdminUserEventDataService = _
  lazy val entities = new MongoDaoHelper("adm_event")

  @Test
  def saveAndRead {
    val entity = new AdminUserEventFixture().event
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def representation {
    val entity = new AdminUserEventFixture().event
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println("repr=" + repr)
    jsonAssert(repr)( s"""{
      "_id" : { "$$oid" : "${entity.id.id}" } ,
      "uid" : ${entity.adminUserId.id.mongoRepr},
      "t" : ${entity.createdTime.mongoRepr},
      "m" : "${entity.message}"
    }""")
  }

  @Test
  def findByUser {

    val userId = AdminUserId()
    val events = (1 to 3).map(i => new AdminUserEventFixture().event.copy(adminUserId = userId, message = s"some message $i"))
    events.foreach(service.save)

    val userEvents = service.findByUser(userId)
    userEvents must haveSize(3)
    userEvents must haveTheSameElementsAs(events)

  }

  @Test
  def delete {
    val entity1 = new AdminUserEventFixture().event
    val entity2 = new AdminUserEventFixture().event
    service.save(entity1)
    service.save(entity2)
    service.delete(entity1.adminUserId ,entity1.id)
    service.findOne(entity1.id) === None
    service.findOne(entity2.id) === Some(entity2)
  }

  @Test
  def deleteByUser {

    val userId = AdminUserId()
    val events = (1 to 3).map(i => new AdminUserEventFixture().event.copy(adminUserId = userId, message = s"some message $i"))
    events.foreach(service.save)

    service.deleteByUser(userId)
    val userEvents = service.findByUser(userId)
    userEvents must haveSize(0)

  }

}

class AdminUserEventFixture {

  val event = AdminUserEvent(
    adminUserId = AdminUserId(),
    message = "Event message"
  )
}
