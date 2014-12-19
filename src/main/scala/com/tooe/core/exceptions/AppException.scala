package com.tooe.core.exceptions

import spray.http.{StatusCodes, StatusCode}

abstract class AppException(msg: String) extends Exception(msg) {
  def statusCode: StatusCode
  def errorCode: Int
  def message: String
}

case class NotFoundException(message: String, errorCode: Int = 0) extends AppException(message) {
  def statusCode = StatusCodes.NotFound
}

object NotFoundException {
  def fromId(id: Any, errorCode: Int = 0) = NotFoundException("Not found: " + id, errorCode)
}

case class UserNotFoundException(message: String) extends AppException(message) {
  def statusCode = StatusCodes.BadRequest
  def errorCode = 142
}

case class BadRequestException(message: String) extends AppException(message) {
  def statusCode = StatusCodes.BadRequest
  def errorCode = 0
}

case class ApplicationException
(
  errorCode: Int = 0,
  message: String = "",
  statusCode: StatusCode = StatusCodes.BadRequest) extends AppException(message)

case class ForbiddenAppException(message: String = "", errorCode: Int = 0) extends AppException(message) {
  def statusCode = StatusCodes.Forbidden
}

case class ConflictAppException(message: String = "", errorCode: Int = 0) extends AppException(message) {
  def statusCode = StatusCodes.Conflict
}