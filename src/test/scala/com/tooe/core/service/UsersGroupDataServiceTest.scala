package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.junit.Test
import com.mongodb.BasicDBObject
import com.tooe.core.db.mongo.domain.UsersGroup
import com.tooe.core.domain.UsersGroupId
import com.tooe.core.util.{Lang, HashHelper}

class UsersGroupDataServiceTest extends SpringDataMongoTestHelper {

  @Autowired var service: UsersGroupDataService = _

  lazy val entities = new MongoDaoHelper("usersgroup").collection

  @Test
  def saveAndReadAndDelete() {
    val entity = new UserGruopFixture().usergroup
    service.findAll.contains(entity) === false
    service.save(entity)
    service.findAll.contains(entity) === true
    entities.remove(new BasicDBObject("_id", entity.id.id))
    service.findAll.contains(entity) === false
  }

  @Test
  def representation {
    val entity = new UserGruopFixture().usergroup
    service.save(entity)
    val repr = entities.findOne(new BasicDBObject("_id", entity.id.id))
    jsonAssert(repr)( s"""{
       "_id" : ${entity.id.id} ,
       "n" : { "ru" : "Крутая группа" }
    }""")
    entities.remove(new BasicDBObject("_id", entity.id.id))
  }

}

class UserGruopFixture {
  val usergroup = UsersGroup(UsersGroupId(HashHelper.uuid), Map(Lang.ru -> "Крутая группа"))
}
