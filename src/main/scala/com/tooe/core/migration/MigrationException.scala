package com.tooe.core.migration
import com.tooe.core.exceptions.AppException
import spray.http.{StatusCodes, StatusCode}

class MigrationException(msg: String) extends AppException(msg){
  def statusCode: StatusCode = StatusCodes.InternalServerError
  def errorCode: Int = 0
  def message: String = "Migration failed: " + msg
}
