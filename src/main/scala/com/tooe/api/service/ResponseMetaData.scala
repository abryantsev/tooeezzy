package com.tooe.api.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.api.validation.ErrorMessage
import com.tooe.core.db.mongo.util.UnmarshallerEntity

trait SuccessfulResponse extends UnmarshallerEntity {
  @JsonProperty("meta")
  val meta = SuccessResponseMetaData
}

object SuccessfulResponse extends SuccessfulResponse

sealed trait ResponseMetaData {
  @JsonProperty("status") def status: String
}

case class FailureResponse(@JsonProperty("meta") meta: ErrorResponseMetaData)

object FailureResponse {
  def apply(messages: ErrorMessage*) = new FailureResponse(ErrorResponseMetaData(messages = messages))
}

case object SuccessResponseMetaData extends ResponseMetaData {
  def status: String = "success"
}

case class ErrorResponseMetaData
(
  @JsonProperty("status") status: String = "error",
  @JsonProperty("errors") messages: Seq[ErrorMessage]
  ) extends ResponseMetaData