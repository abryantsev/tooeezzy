package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain._
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.domain.{PromotionStatus, UserId, PromotionId, PromotionVisitorId}
import java.util.Date
import com.tooe.api.service.OffsetLimit
import com.tooe.core.db.mongo.query.UpdateResult
import com.tooe.core.util.TestDateGenerator

class PromotionVisitorDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: PromotionVisitorDataServiceImpl = _

  lazy val entities = new MongoDaoHelper("promotion_visitor")

  def fullEntity = PromotionVisitor(
    id = PromotionVisitorId(),
    promotion = PromotionId(),
    visitor = UserId(),
    status = PromotionStatus.Confirmed,
    time = new Date
  )

  @Test
  def saveAndRead {
    val entity = fullEntity
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def representation {
    val entity = fullEntity
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println("repr="+repr)
    jsonAssert(repr)(s"""{
      "_id" : { $$oid : "${entity.id.id.toString}" } ,
      "pid" : { $$oid : "${entity.promotion.id.toString}" } ,
      "uid" : { $$oid : "${entity.visitor.id.toString}" } ,
      "s" : ${entity.status.id} ,
      "t" : ${entity.time.mongoRepr}
    }""")
  }

  @Test
  def upsert {
    val confirmDate = new TestDateGenerator().next()
    val entity = fullEntity

    service.find(entity.promotion, entity.visitor) === None
    service.upsertStatus(entity.promotion, entity.visitor, confirmDate, PromotionStatus.Confirmed) === UpdateResult.NotFound

    val found1 = service.find(entity.promotion, entity.visitor).get
    found1.promotion === entity.promotion
    found1.visitor === entity.visitor
    found1.status === PromotionStatus.Confirmed
    found1.time === confirmDate
  }

  @Test
  def findAllVisitors {
    val entity = fullEntity
    service.save(entity)

    service.findAllVisitors(entity.promotion, OffsetLimit()) == Seq(entity)
  }

  @Test
  def findAllVisitorIds {
    val entity = fullEntity
    service.findAllVisitorIds(entity.promotion) === Set()

    service.save(entity)
    service.findAllVisitorIds(entity.promotion) === Set(entity.visitor)
  }
  
  @Test
  def countAllVisitors {
    val entity = fullEntity
    service.countAllVisitors(entity.promotion) === 0
    
    service.save(entity)
    service.countAllVisitors(entity.promotion) === 1
  }

  @Test
  def findByPromotions {
    val p1, p2 = fullEntity
    Seq(p1, p2) foreach service.save

    service.findVisitors(Set(p1.promotion, p2.promotion)).toSet === Set(p1, p2)
    service.findVisitors(Set(p1.promotion)) === Seq(p1)
  }
}