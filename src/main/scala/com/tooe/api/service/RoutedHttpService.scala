package com.tooe.api.service

import akka.actor.Actor
import spray.routing._
import directives.LogEntry
import spray.http.HttpRequest
import com.tooe.core.db.mongo.util.JacksonModuleScalaSupport
import com.tooe.api.validation.ErrorMessage
import com.tooe.core.exceptions.AppException
import spray.http.StatusCodes._
import spray.routing.MissingFormFieldRejection
import spray.routing.MalformedHeaderRejection
import spray.routing.MalformedRequestContentRejection
import spray.routing.MalformedQueryParamRejection
import com.tooe.api.validation.ValidationException
import spray.routing.MissingHeaderRejection
import spray.routing.MissingCookieRejection
import spray.routing.MalformedFormFieldRejection
import spray.routing.ValidationRejection
import spray.routing.MissingQueryParamRejection

class RoutedHttpService(route: Route) extends Actor with HttpService with JacksonModuleScalaSupport {

  implicit val exceptionHandler = ExceptionHandler {
    case ValidationException(result, statusCode) => ctx =>
      ctx.complete(statusCode, result.badRequestResponse)
    case ex: AppException => ctx =>
      ctx.complete(ex.statusCode, FailureResponse(ErrorMessage(ex.message, ex.errorCode)))
  }

  implicit val rejectionHandler = RejectionHandler {
      case Nil | MethodRejection(_) :: _ ⇒ complete(NotFound, FailureResponse(ErrorMessage("The requested resource could not be found.", 0)))

      case MalformedFormFieldRejection(name, msg, _) :: _ ⇒
        complete(BadRequest, FailureResponse(ErrorMessage("The form field '" + name + "' was malformed:\n" + msg, 0)))

      case MalformedHeaderRejection(headerName, msg, _) :: _ ⇒
        complete(BadRequest, FailureResponse(ErrorMessage(s"The value of HTTP header '$headerName' was malformed:\n" + msg, 0)))

      case MalformedQueryParamRejection(name, msg, _) :: _ ⇒
        complete(BadRequest, FailureResponse(ErrorMessage("\"The query parameter '\"" + name + "' was malformed:\n" + msg, 0)))

      case MalformedRequestContentRejection(msg, _) :: _ ⇒
        complete(BadRequest, FailureResponse(ErrorMessage("The request content was malformed:\n" + msg, 0)))

      case MissingCookieRejection(cookieName) :: _ ⇒
        cookieName match {
          case SessionCookies.Token | AdminSessionCookies.Token => complete(Unauthorized, FailureResponse(ErrorMessage("Request is missing required cookie '" + cookieName + '\'', 0)))
          case _ => complete(BadRequest, FailureResponse(ErrorMessage("Request is missing required cookie '" + cookieName + '\'', 0)))
        }

      case MissingFormFieldRejection(fieldName) :: _ ⇒
        complete(BadRequest, FailureResponse(ErrorMessage("Request is missing required form field '" + fieldName + '\'', 0)))

      case MissingHeaderRejection(headerName) :: _ ⇒
        complete(BadRequest, FailureResponse(ErrorMessage("Request is missing required HTTP header '" + headerName + '\'', 0)))

      case MissingQueryParamRejection(paramName) :: _ ⇒
        complete(NotFound, FailureResponse(ErrorMessage( "Request is missing required query parameter '" + paramName + '\'', 0)))

      case RequestEntityExpectedRejection :: _ ⇒
        complete(BadRequest, FailureResponse(ErrorMessage("Request entity expected but not supplied", 0)))

      case ValidationRejection(msg, _) :: _ ⇒
        complete(BadRequest, FailureResponse(ErrorMessage(msg, 0)))
  }

  implicit def actorRefFactory = context

  def receive = runRoute(
    logRequest(showRequest _) {
      route
    }
  )

  import akka.event.Logging._
  def showRequest(request: HttpRequest) = LogEntry(request.uri, InfoLevel)
}

trait TooeezzyyRejection extends java.lang.Object with spray.routing.Rejection with scala.Product with scala.Serializable
case class UserNotFoundRejection()  extends TooeezzyyRejection  {
  val message = "User not found"
}

case class RegistrationNotConfirmedRejection()  extends TooeezzyyRejection  {
  val message = "Registration not confirmed"
}