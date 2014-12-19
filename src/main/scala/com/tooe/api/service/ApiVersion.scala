package com.tooe.api.service

private[service] case class ApiVersion(private[service] val id: String)

private[service] object ApiVersions {
  val v01 = ApiVersion("v01")
  val v02 = ApiVersion("v02")
}

object ApiVersion {

  implicit def apiVersionHelper(version: ApiVersion) = new {
    def |(anotherVersion: ApiVersion) = Set(version, anotherVersion)
  }

  implicit def apiVersionSetHelper(versionSet: Set[ApiVersion]) = new {
    def |(version: ApiVersion) = versionSet + version
  }
}