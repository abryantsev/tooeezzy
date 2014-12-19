package com.tooe.core.usecase

import akka.testkit.{TestActorRef, TestKit, TestProbe}
import com.tooe.core.service._
import akka.actor.{Props, ActorSystem}
import com.tooe.core.db.mongo.domain._
import concurrent.Future
import com.tooe.api.service.RegistrationParams
import com.tooe.core.util.Lang
import com.tooe.core.domain.{UserId, CountryId, Gender, RegionId}
import java.util.{Date, UUID}
import com.tooe.core.db.graph.GraphException

class UserWriteActorTest extends ActorTestSpecification {

  "UserWriteActor" should {
    "not call saveUser if GraphActor throw exception during registration process" >> {
      val f = new UserWriteActorFixture {
        val probe = TestProbe()
        override def userWriteActorFactory = new UserWriteActorUnderTest {
          override def putUserInGraph(userId: UserId) = Future failed new GraphException("")
          override def createUser(userId: UserId, region: Region, country: Country, params: RegistrationParams)(implicit lang: Lang): User = {
            probe.ref ! "createUser has called"
            throw new IllegalStateException("this method shouldn't have been called")
          }
          override def saveUser(user: User): Future[User] = super.saveUser(user)
        }
      }
      f.userWriteActor ! UserWriteActor.CreateNewUser(f.registrationParams, Lang.ru)
      f.probe.expectNoMsg()
      success
    }
  }

  step {
    system.shutdown()
  }
}

abstract class UserWriteActorFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  val registrationParams = RegistrationParams(
    registrationEmail = UUID.randomUUID().toString + "@mail.ru",
    pwd = UUID.randomUUID().toString,
    name = "name",
    lastName = "lastname",
    regionId = RegionId(),
    gender = Gender.Female,
    birthday = new Date()
  )

  val country = new CountryFixture().country
  val region = new RegionFixture().region

  class UserWriteActorUnderTest extends UserWriteActor {
    override def getCountry(id: CountryId) = Future successful country
    override def getRegion(id: RegionId) = Future successful region
    override def isFunctionalPhoneAlreadyExists(phone: Option[String], countryCode: Option[String])(implicit lang: Lang) = Future successful false
  }

  def userWriteActorFactory = new UserWriteActorUnderTest

  lazy val userWriteActor = TestActorRef[UserWriteActorUnderTest](Props(userWriteActorFactory))
}