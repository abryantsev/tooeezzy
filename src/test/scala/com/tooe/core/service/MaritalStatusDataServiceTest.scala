package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain._
import com.tooe.core.util.HashHelper
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.domain.{Gender, MaritalStatusId}

class MaritalStatusDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: MaritalStatusDataServiceImpl = _

  lazy val entities = new MongoDaoHelper("maritalstatus")

  @Test
  def saveAndRead {
    val entity = new MaritalStatusFixture().maritalStatus()
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def representation {
    val entity = new MaritalStatusFixture().representationMaritalStatus
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println(repr)
    jsonAssert(repr)(s"""{
       "_id" : ${entity.id.id} ,
       "n" : { "en" : "Single", "ru" : "Холост" } ,
       "nf" : { "en" : "Single", "ru" : "Не замужем" }
    }""")
  }

  @Test
  def findByIds {
    val entity = new MaritalStatusFixture().maritalStatus()
    val ids = Set(entity.id)
    service.find(ids) === Nil
    service.save(entity)
    service.find(ids) === Seq(entity)
  }

}

class MaritalStatusFixture {
  val representationMaritalStatus = MaritalStatus(id = MaritalStatusId(HashHelper.uuid),
                                                  name = Map("en" -> "Single", "ru" -> "Холост"),
                                                  femaleStatusName = Map("en" -> "Single", "ru" -> "Не замужем"))

  def maritalStatus(gender: Option[Gender] = None) = {
    def genderIsFemale = gender == Some(Gender.Female)

    val (name, femaleStatusName): (ObjectMap[String], ObjectMap[String]) =
      if(genderIsFemale)
        (ObjectMap.empty[String], Map("en" -> "Single", "ru" -> "Не замужем"))
      else
        (Map("en" -> "Single", "ru" -> "Холост"), ObjectMap.empty[String])

    MaritalStatus(id = MaritalStatusId(HashHelper.uuid),
                  name = name,
                  femaleStatusName = femaleStatusName)
  }

}