package com.tooe.api.validation

import com.tooe.api.service.FailureResponse
import spray.http.{StatusCodes, StatusCode}

sealed trait ValidationResult {
  def succeed: Boolean
  def messages: Seq[ErrorMessage]
  def &(that: ValidationResult): ValidationResult

  def failed: Boolean = !succeed

  def require(condition: => Boolean, errorMsg: => String, errorCode: Int = 0): ValidationResult =
    if (condition) this
    else this & ValidationFailed(ErrorMessage(errorMsg, errorCode) :: Nil)

  def throwIfFailed(statusCode: StatusCode)
}

object ValidationSucceed extends ValidationResult {
  def succeed = true
  def messages = Nil

  def &(that: ValidationResult) = that
  def throwIfFailed(statusCode: StatusCode) {}
}

case class ValidationFailed(messages: Seq[ErrorMessage]) extends ValidationResult {
  def succeed = false

  def &(that: ValidationResult) = that match {
    case ValidationSucceed => this
    case ValidationFailed(ms) => ValidationFailed(ms ++ messages)
  }

  def badRequestResponse = FailureResponse(messages: _*)
  def throwIfFailed(statusCode: StatusCode) {
    throw ValidationException(this, statusCode)
  }
}

object ValidationFailed {
  def apply(em: ErrorMessage): ValidationFailed = new ValidationFailed(em :: Nil)
  def apply(message: String, code: Int = 0): ValidationFailed = apply(ErrorMessage(message, code))
}