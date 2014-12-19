package com.tooe.api.service

import spray.http.StatusCodes

class MainPrefixDirectiveTest extends HttpServiceTest {

  val v01 = ApiVersion("v01")
  val v02 = ApiVersion("v02")

  "mainPrefix" should {
    val body = complete(StatusCodes.OK)
    "any version match" >> {
      Get("/v05/ru/path") ~> (mainPrefix & path("path")) { rc => rc.version.id === "v05"; body } ~> check { handled === true }
    }
    "one version match" >> {
      Get("/v01/ru/path") ~> (mainPrefix(v01) & path("path")) { _ => body } ~> check { handled === true }
    }
    "one version mismatch" >> {
      Get("/v02/ru/path") ~> (mainPrefix(v01) & path("path")) { _ => body } ~> check { handled === false }
    }
    "one of match v01" >> {
      Get("/v01/ru/path") ~> (mainPrefix(v01 | v02) & path("path")) { _ => body } ~> check { handled === true }
    }
    "one of match v02" >> {
      Get("/v02/ru/path") ~> (mainPrefix(v01 | v02) & path("path")) { _ => body } ~> check { handled === true }
    }
    "one of mismatch" >> {
      Get("/v03/ru/path") ~> (mainPrefix(v01 | v02) & path("path")) { _ => body } ~> check { handled === false }
    }
  }
}