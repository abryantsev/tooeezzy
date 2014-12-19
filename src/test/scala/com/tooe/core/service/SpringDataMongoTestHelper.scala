package com.tooe.core.service

import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.ContextConfiguration
import org.specs2.matcher.JUnitMustMatchers
import com.mongodb.DBObject
import org.skyscreamer.jsonassert.JSONAssert
import com.tooe.core.infrastructure.AppContextTestHelper

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations = Array(AppContextTestHelper.TestMongoContext))
abstract class SpringDataMongoTestHelper extends JUnitMustMatchers with JsonTestHelper

trait JsonTestHelper {
  def jsonAssert(obj: DBObject, strict: Boolean = true)(expected: String) {
    val repr = com.mongodb.util.JSON.serialize(obj)
    JSONAssert.assertEquals(expected, repr, strict)
  }
}