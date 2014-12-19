package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain._
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.util.HashHelper
import com.tooe.core.domain._

class UserPhoneDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: UserPhoneDataServiceImpl = _

  lazy val entities = new MongoDaoHelper("user_phones")

  @Test
  def saveAndRead {
    val entity = UserPhone(userId = UserId(), phone = Phone(number = HashHelper.uuid))
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def representation {
    val userPhone = new UserPhoneFixture().userPhone
    service.save(userPhone)
    val repr = entities.findOne(userPhone.id.id)
    jsonAssert(repr)(s"""{
      "_id" : ${userPhone.id.id.mongoRepr} ,
      "uid" : ${userPhone.userId.id.mongoRepr} ,
      "p" : {
        "c" : "7" ,
        "n" : "9999999999" ,
        "p" : "phone-purpose"
        }
      }
      """)
  }

  @Test
  def findUserPhones() {
    val userPhone = new UserPhoneFixture().userMainPhone
    service.save(userPhone)
    service.findUserPhone(userPhone.phone.countryCode, userPhone.phone.number) === Some(userPhone)
  }

  @Test
  def findNonUniquePhones() {
    val userId = UserId()
    val p1, p2 = new PhoneShortFixture().phoneShort

    service.saveUserPhones(userId, Seq(p1))
    service.deleteUserPhones(UserId())

    service.findNonUniquePhones(userId, Seq(p1, p2)) === Seq()
    service.findNonUniquePhones(UserId(), Seq(p1, p2)) === Seq(p1)

    service.deleteUserPhones(userId)
    service.findNonUniquePhones(userId, Seq(p1, p2)) === Seq()
  }
}