package com.tooe.api.service

import spray.routing.Directives


trait DigitalSignHelper {
  self: Directives with SettingsHelper =>

  val optionalDigitalSign = optionalHeaderValueByName(settings.Security.TooeDSignHeaderName).as(DigitalSign)

  def checkDigitalSign[T](dsignOpt: Option[DigitalSign])(f: Either[String, Option[DigitalSign]] => T) =
    f(dsignOpt.map(_.signature.map {
      case value if value == settings.Security.TooeDSignPassword => Right(dsignOpt)
      case _ => Left("Wrong security header value")
    }.getOrElse(Left("Security header value is not defined"))).getOrElse(Right(dsignOpt)))

}