package com.tooe.core.service

import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.ContextConfiguration
import org.specs2.matcher.JUnitMustMatchers
import com.tooe.core.infrastructure.AppContextTestHelper

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations = Array(AppContextTestHelper.TestMySqlContext))
abstract class SpringDataMySqlTestHelper extends JUnitMustMatchers