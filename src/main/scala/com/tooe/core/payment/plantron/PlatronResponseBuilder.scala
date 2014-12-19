package com.tooe.core.payment.plantron

import com.tooe.core.util.HashHelper

object PlatronResponseBuilder extends PlatronMessageBuilder {

  def build(response: PlatronResponse): PlatronParams = {
    import response._
    Seq(
      'pg_status -> status.id,
      'pg_salt -> salt
    ) ++
    optParam('pg_timeout, timeout map (_.toString)) ++
    optParam('pg_description, description) ++
    optParam('pg_error_description, errorDescription)
  }
}

case class PlatronResponse
(
  status: PlatronResponseStatus,
  timeout: Option[Int] = None,
  description: Option[String] = None,
  errorDescription: Option[String] = None,
  salt: String = HashHelper.uuid
  )

trait PlatronResponseStatus {
  def id: String
}

object PlatronResponseStatus {
  object Ok extends PlatronResponseStatus {
    def id: String = "ok"
  }
  object Reject extends PlatronResponseStatus {
    def id: String = "rejected"
  }
  object Error extends PlatronResponseStatus {
    def id: String = "error"
  }

  val values: Seq[PlatronResponseStatus] = Ok :: Reject :: Error :: Nil

  val fromId = values.map(x => x.id -> x).toMap
}