package com.tooe.core.migration.infrastructure

import org.specs2.matcher.JUnitMustMatchers
import com.tooe.core.service.JsonTestHelper
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.ContextConfiguration
import scala.Array
import com.tooe.core.infrastructure.AppContextTestHelper

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations = Array(AppContextTestHelper.TestMongoContext, MigrationAppContextTestHelper.testMigrationContext))
class MigrationSpringDataMongoTestHelper extends JUnitMustMatchers with JsonTestHelper