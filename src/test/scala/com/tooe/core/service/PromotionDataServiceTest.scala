package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain._
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.util.{Lang, DateHelper, SomeWrapper}
import java.util.Date
import com.tooe.core.domain._
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.domain.Promotion
import com.tooe.core.domain.LocationCategoryId
import com.tooe.core.domain.RegionId
import com.tooe.core.domain
import com.tooe.core.usecase.{PromotionChangeRequest, SearchPromotionsRequest}
import com.tooe.core.usecase.promotion.PromotionSearchSortType
import com.tooe.api.service.OffsetLimit
import com.tooe.core.domain.Unsetable.Update

class PromotionDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: PromotionDataServiceImpl = _

  lazy val entities = new MongoDaoHelper("promotion")
  import SomeWrapper._

  @Test
  def saveAndRead {
    val entity = new PromotionFixture().entity
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def representation {
    val entity = new PromotionFixture().entity
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println("repr="+repr)
    jsonAssert(repr)(s"""{
      "_id" : { "$$oid" : "${entity.id.id.toString}" } ,
      "n" : { "ru" : "name" } ,
      "d" : { "ru" : "description" } ,
      "i" : { "ru" : "additionalInfo" } ,
      "pm" : [ { "u" : "some-url" } ] ,
      "ds" : {
        "st" : ${entity.dates.start.mongoRepr} ,
        "et" : ${entity.dates.end.get.mongoRepr} ,
        "at" : ${entity.dates.time.get.mongoRepr} ,
        "p" : ${entity.dates.period.id}
      } ,
      "pc" : { "ru" : "free"} ,
      "l" : {
        "lid" : { "$$oid" : "${entity.location.location.id.toString}" } ,
        "rid" : { "$$oid" : "${entity.location.region.id.toString}" } ,
        "n" : { "ru" : "locationName" } ,
        "lc" : [ "shops" ]
      },
      "vc" : 5
    }""")
  }

  @Test
  def incrementVisitorsCounter {
    val entity = new PromotionFixture().entity.copy(visitorsCount = 7)
    service.save(entity)
    entity.visitorsCount === 7
    service.incrementVisitorsCounter(entity.id, -5)
    service.findOne(entity.id).get.visitorsCount === 2
  }

  @Test
  def searchPromotions {
    import DateHelper._
    val entity, entity2 = new PromotionFixture().entity
    service.save(entity)
    service.save(entity2)

    val request = SearchPromotionsRequest(
      entity.location.region,
      entity.location.categories.map(_.id).headOption,
      entity.name("ru"),
      entity.dates.start.addHours(5),
      Option(PromotionSearchSortType.Name),
      None,
      Option(Set(PromotionFields.PromotionsShort)),
      OffsetLimit(0, 20)
    )
    service.searchPromotions(request, "ru" ) === Seq(entity)
  }

  @Test
  def exists {
    val entity = new PromotionFixture().entity
    service.exists(entity.id) === false
    service.save(entity)
    service.exists(entity.id) === true
  }

  @Test
  def find {
    val f = new PromotionFixture
    import f._
    service.find(Set(entity.id)) === Seq()
    service.save(entity)
    service.find(Set(entity.id)) === Seq(entity)
  }

  @Test
  def update {
    val entity = (new PromotionFixture).entity
    service.save(entity)

    implicit val lang = Lang.ru

    val request = PromotionChangeRequest(name = Some("new name"),
                                        description = Some("new description"),
                                        additionalInformation = Some("new information"),
                                        startDate = Some(new Date),
                                        endDate = Update(new Date),
                                        time = Update(new Date),
                                        period = Update(PromotionPeriod.Day),
                                        price = Some("free"),
                                        media = Some(MediaUrl("file url")))

    service.update(entity.id, request, lang)

    val updatedEntity = service.findOne(entity.id)

    updatedEntity.flatMap(_.name.localized) === request.name
    updatedEntity.flatMap(_.description.localized) === request.description
    updatedEntity.flatMap(_.additionalInfo.flatMap(_.localized)) === request.additionalInformation
    updatedEntity.flatMap(_.dates.start) === request.startDate
    updatedEntity.flatMap(_.dates.end) === Some(request.endDate.get)
    updatedEntity.flatMap(_.dates.time) === Some(request.time.get)
    updatedEntity.flatMap(_.dates.period) === Some(request.period.get)
    updatedEntity.flatMap(_.price.flatMap(_.localized)) === request.price
    updatedEntity.flatMap(_.media.headOption) === request.media
  }
}

class PromotionFixture(locationId: LocationId = LocationId(new ObjectId)) {
  import SomeWrapper._
  import DateHelper._

  val entity = Promotion(
    name = Map("ru" -> "name"),
    description = Map("ru" -> "description"),
    additionalInfo = Option(Map("ru" -> "additionalInfo")),
    media = Seq(domain.MediaUrl("some-url")),
    dates = promotion.Dates(
      start = new Date,
      end = new Date().addHours(10),
      time = new Date,
      period = PromotionPeriod.Week
    ),
    price = Option(Map("ru" -> "free")),
    location = promotion.Location(
      location = locationId,
      name = Map("ru" -> "locationName"),
      region = RegionId(new ObjectId),
      categories = Seq(LocationCategoryId("shops"))
    ),
    visitorsCount = 5
  )
}