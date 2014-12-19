package com.tooe.core.usecase.payment

case class CallbackParams
(
  system: Option[String],
  action: Option[String]
  )