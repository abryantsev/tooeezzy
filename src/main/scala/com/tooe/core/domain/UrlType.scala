package com.tooe.core.domain

case class UrlType(id: String)

object UrlType {
  val cdn = UrlType("cdn")
  val s3 = UrlType("s3")
  val static = UrlType("static")
  val http = UrlType("http")

  val MigrationType = Some(http)
}
