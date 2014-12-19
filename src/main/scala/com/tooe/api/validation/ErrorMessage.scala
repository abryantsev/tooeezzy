package com.tooe.api.validation

case class ErrorMessage(message: String, code: Int)

object ErrorMessage {
  def notSpecifiedField(path: String) = ErrorMessage(path+": required field should be specified", 0)
}