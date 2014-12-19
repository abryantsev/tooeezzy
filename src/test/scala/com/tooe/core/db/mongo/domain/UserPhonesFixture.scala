package com.tooe.core.db.mongo.domain

class UserPhonesFixture {

  val phones = new PhoneFixture()
  import  phones._

  val userPhones = UserPhones(all = Option(Seq(functionalPhone)), main = Option(mainFunctionalPhone))
}