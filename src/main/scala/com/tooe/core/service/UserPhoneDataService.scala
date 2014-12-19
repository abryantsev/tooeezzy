package com.tooe.core.service

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.UserPhoneRepository
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.db.mongo.domain.{Phone, UserPhone}
import com.tooe.core.domain.{PhoneShort, UserId, UserPhoneId}
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import com.tooe.core.db.mongo.converters.PhoneConverter
import scala.collection.JavaConverters._

trait UserPhoneDataService {
  def save(entity: UserPhone): UserPhone

  def findOne(id: UserPhoneId): Option[UserPhone]

  def findUserPhone(countryCode: String, phoneNumber: String): Option[UserPhone]

  def saveUserPhones(userId: UserId, phones: Seq[PhoneShort]): Unit

  def findNonUniquePhones(userId: UserId, phones: Seq[PhoneShort]): Seq[PhoneShort]

  def deleteUserPhones(userId: UserId): Unit
}

@Service
class UserPhoneDataServiceImpl extends UserPhoneDataService {
  @Autowired var repo: UserPhoneRepository = _
  @Autowired var mongo: MongoTemplate = _

  def save(entity: UserPhone) = repo.save(entity)

  def findOne(id: UserPhoneId) = Option(repo.findOne(id.id))

  def findUserPhone(countryCode: String, phoneNumber: String): Option[UserPhone] =
    Option(repo.findUserPhoneByCountryCodeAndNumber(countryCode, phoneNumber))

  override def saveUserPhones(userId: UserId, phones: Seq[PhoneShort]): Unit = {
    val vs = phones map (ps => UserPhone(userId = userId, phone = Phone(ps)))
    repo.save(vs.asJavaCollection)
  }

  override def findNonUniquePhones(userId: UserId, phones: Seq[PhoneShort]) = {
    val ps = phones.map(p => PhoneConverter.PhoneConverter.serialize(Phone(p))).asJavaCollection
    val criteria = Criteria.where("uid").`ne`(userId.id).and("p").in(ps)
    val query = Query.query(criteria)
    mongo.find(query, classOf[UserPhone]).asScala map (_.phone.toPhoneShort)
  }

  override def deleteUserPhones(userId: UserId) = {
    val criteria = Criteria.where("uid").is(userId.id)
    val query = Query.query(criteria)
    mongo.remove(query, classOf[UserPhone])
  }
}