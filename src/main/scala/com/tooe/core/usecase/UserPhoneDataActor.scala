package com.tooe.core.usecase

import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.db.mongo.domain._
import com.tooe.core.service.UserPhoneDataService
import concurrent.Future
import com.tooe.core.application.Actors
import com.tooe.core.domain.{PhoneShort, UserId}

object UserPhoneDataActor{
  final val Id = Actors.UserPhoneData

  case class UserPhoneExist(countryCode: String, phoneNumber: String)
  case class FindUserPhone(countryCode: String, phoneNumber: String)
  case class SaveUserPhone(userId: UserId, phone: Phone)

  case class FindNonUniquePhones(userId: UserId, phones: Seq[PhoneShort])
  case class UpdateUserPhones(userId: UserId, phones: Seq[PhoneShort])
}

class UserPhoneDataActor extends AppActor {

  private lazy val service = BeanLookup[UserPhoneDataService]

  import UserPhoneDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  def receive = {
    case UserPhoneExist(countryCode, phoneNumber) =>
      Future(service.findUserPhone(countryCode, phoneNumber)) map { phoneOpt =>
        phoneOpt.isDefined
      } pipeTo sender

    case FindUserPhone(countryCode, phoneNumber) =>
      Future{
        service.findUserPhone(countryCode, phoneNumber)
      } pipeTo sender

    case SaveUserPhone(userId: UserId, fullPhone) =>
      Future{
        service.save(UserPhone(userId = userId, phone= fullPhone))
      } pipeTo sender

    case FindNonUniquePhones(userId, phones) => Future(service.findNonUniquePhones(userId, phones)) pipeTo sender

    case UpdateUserPhones(userId, phones) =>
      service.deleteUserPhones(userId)
      service.saveUserPhones(userId, phones)
  }
}
