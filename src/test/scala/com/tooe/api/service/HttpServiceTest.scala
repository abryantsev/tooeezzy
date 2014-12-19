package com.tooe.api.service

import org.specs2.matcher.MustMatchers
import spray.testkit.Specs2RouteTest
import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.tooe.core.util.Lang

@RunWith(classOf[JUnitRunner])
class HttpServiceTest extends Specs2RouteTest with MustMatchers with Specification with SprayServiceBaseHelper {

  implicitly[RouteTestTimeout]
  implicitly[spray.routing.RoutingSettings]
  implicitly[spray.util.LoggingContext]

  sequential

  def routeContext = RouteContext(versionId = ApiVersions.v01.id, langId = Lang.ru)
  def urlPrefix = s"/${routeContext.versionId}/${routeContext.langId}/"
  def randomUUID = java.util.UUID.randomUUID().toString
}