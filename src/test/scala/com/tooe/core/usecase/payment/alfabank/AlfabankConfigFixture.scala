package com.tooe.core.usecase.payment.alfabank

import com.typesafe.config.ConfigFactory

class AlfabankConfigFixture extends ConfigFixture {

  val configStr = s"""
  payment.alfabank {
    username = "username"
    password = "password"
    registerPreAuthUrl = "http://registerPreAuthUrl"
    depositUrl ="http://depositUrl"
    getOrderStatusExtendedUrl = "http://getOrderStatusExtendedUrl"

    timeout = 15 minutes

    timeout-before-check-status = 1 second

    returnUrl = "http://returnUrl/$$LANG/$$ORDER"
    successUrl = "http://successUrl/$$LANG/$$ORDER"
    failureUrl = "http://failureUrl/$$LANG/$$ORDER"
  }
  """
}

trait ConfigFixture {
  def configStr: String
  lazy val config = ConfigFactory.parseString(configStr)
}