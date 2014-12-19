package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.junit.Test
import com.tooe.core.db.mongo.domain.Urls
import com.tooe.core.domain.{UrlType, MediaObjectId, EntityType}
import org.bson.types.ObjectId
import java.util.{Date, UUID}

class UrlsDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: UrlsDataService = _
  lazy val entities = new MongoDaoHelper("urls")

  @Test
  def saveAndRead {
    val entity = new UrlsFixture().url
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
    service.delete(entity.id)
    service.findOne(entity.id) === None
  }

  @Test
  def representation {
    val entity = new UrlsFixture().url
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println("repr="+repr)
    jsonAssert(repr)(s"""{
      "_id" : ${entity.id.id.mongoRepr} ,
      "e" : "${entity.entityType.id}" ,
      "eid" : ${entity.entityId.mongoRepr} ,
      "t" : ${entity.time.mongoRepr} ,
      "uri" : "${entity.mediaId.id}" ,
      "ut" : "${entity.urlType.map(_.id).getOrElse("")}"
    }""")
  }

  @Test
  def getLastUrls {
    val smallSize = 1
    val largeSize = 1000
    val urlType = UrlType.s3
    service.getLastUrls(smallSize, urlType).size must beLessThanOrEqualTo(smallSize)
    service.getLastUrls(largeSize, urlType).size must beLessThanOrEqualTo(largeSize)
  }

  @Test
  def setReadTime {
    val url = new UrlsFixture().url
    service.save(url)

    val readTime = new Date

    service.setReadTime(Seq(url.id), readTime)

    service.findOne(url.id).flatMap(_.readTime) === Some(readTime)
  }

  @Test
  def deleteByEntityIdAndUrl {
    val urls = generateUrls(3)

    urls.foreach(service.save)

    val firstPartUrls = urls.take(2)

    val lastUrl = urls.drop(2).head

    service.delete(firstPartUrls.map(u => u.entityId -> u.mediaId))

    firstPartUrls.foreach(u => service.findOne(u.id) === None)

    service.findOne(lastUrl.id) === Some(lastUrl)

  }

  def generateUrls(count: Int) = (1 to count) map {_ => new UrlsFixture().url }

}

class UrlsFixture {

  val url = Urls(
    entityType = EntityType.company,
    entityId = new ObjectId(),
    mediaId = MediaObjectId("url_suffix:" + UUID.randomUUID().toString)
  )

}
