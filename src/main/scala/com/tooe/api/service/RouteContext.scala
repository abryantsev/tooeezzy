package com.tooe.api.service

import com.tooe.core.util.Lang

trait RouteContext {
  implicit def version: ApiVersion
  implicit def lang: Lang

  def versionId: String //TODO for backwards compatibility
  def langId: String //TODO for backwards compatibility
}

object RouteContext {
  @deprecated("Use VersionLang")
  def apply(versionId: String, langId: String): RouteContext = RouteContextOld(versionId, langId)
  @deprecated("Use VersionLang")
  def apply(versionId: String, lang: Lang): RouteContext = RouteContextOld(versionId, lang.id)
}

case class VersionLang(version: ApiVersion, lang: Lang) extends RouteContext {
  def versionId = version.id
  def langId = lang.id
}

@deprecated("VersionLang is going to replace it")
case class RouteContextOld(versionId: String, langId: String) extends RouteContext {
  implicit def lang: Lang = Lang(langId)
  implicit def version = ApiVersion(versionId)
}