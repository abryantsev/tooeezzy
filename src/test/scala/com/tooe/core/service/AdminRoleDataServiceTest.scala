package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.junit.Test
import com.mongodb.BasicDBObject
import com.tooe.core.db.mongo.domain.AdminRole
import com.tooe.core.domain.AdminRoleId
import com.tooe.core.util.{Lang, HashHelper}

class AdminRoleDataServiceTest extends SpringDataMongoTestHelper{

  @Autowired var service: AdminRoleDataService = _

  lazy val entities = new MongoDaoHelper("adm_user_role").collection

  @Test
  def saveAndReadAndDelete() {
    val entity = new AdminRoleFixture().adminRole
    service.findAll.contains(entity) === false
    service.save(entity)
    service.findAll.contains(entity) === true
    entities.remove(new BasicDBObject("_id", entity.id.id))
    service.findAll.contains(entity) === false
  }

  @Test
  def representation {
    val entity = new AdminRoleFixture().adminRole
    service.save(entity)
    val repr = entities.findOne(new BasicDBObject("_id", entity.id.id))
    jsonAssert(repr)( s"""{
       "_id" : ${entity.id.id} ,
       "n" : { "ru" : "крутой период" }
    }""")
    entities.remove(new BasicDBObject("_id", entity.id.id))
  }

}

class AdminRoleFixture {
  val adminRole = AdminRole(AdminRoleId(HashHelper.uuid), Map(Lang.ru -> "крутой период"))
}

