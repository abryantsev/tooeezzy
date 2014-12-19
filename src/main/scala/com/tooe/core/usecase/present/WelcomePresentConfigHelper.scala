package com.tooe.core.usecase.present

import akka.actor.Actor
import com.tooe.core.util.DateHelper
import com.tooe.api.service.ObjectId
import com.tooe.core.domain.{ProductId, UserId}

trait WelcomePresentConfigHelper {
  self: Actor =>

  val config = context.system.settings.config
  import config._
  val Presenter = UserId(ObjectId(getString("present.welcome.presenter-uid")))
  val Product = ProductId(ObjectId(getString("present.welcome.product-uid")))
  val ValidFrom = DateHelper.parseDateTime(getString("present.welcome.valid-from"))
  val ValidTill = DateHelper.parseDateTime(getString("present.welcome.valid-till"))
  val Message = getString("present.welcome.message")
}