package com.tooe.core.service

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.junit.{AfterClass, BeforeClass}

@RunWith(classOf[Suite])
@SuiteClasses(Array(
classOf[PaymentDataServiceTest],
classOf[ProductSnapshotDataServiceTest]
))
object MySqlDataServiceSuite {

  @BeforeClass
  def before {
    //TODO check MySql availability
  }

  @AfterClass
  def after {
  }
}
