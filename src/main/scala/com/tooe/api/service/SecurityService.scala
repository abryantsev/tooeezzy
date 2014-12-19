package com.tooe.api.service

import com.tooe.core.usecase._
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.domain._
import com.fasterxml.jackson.annotation.JsonProperty
import spray.routing.PathMatcher
import com.tooe.api.validation.{ValidationContext, Validatable}
import spray.http.StatusCodes
import java.util.Date
import com.tooe.core.db.mongo.domain.Phone

class SecurityService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val securityActor = lookup(SecurityActor.Id)

  import SecurityService._
  import SecurityActor._

  val route =
    (mainPrefix & path(Path.Users)) { routeContext: RouteContext =>
      post {
        entity(as[RegistrationParams]) { params: RegistrationParams =>
            complete(StatusCodes.Created, securityActor.ask(RegisterUser(params, routeContext)).mapTo[RegisterUserResponse])
        }
      }
    } ~
      (mainPrefix & path(Path.ResetCredentials)) { routeContext: RouteContext =>
        post {
          entity(as[PasswordRecoveryRequest]) { prr: PasswordRecoveryRequest =>
              complete(securityActor.ask(PasswordRecovery(prr.email)).mapTo[SuccessfulResponse])
          }
        }
      } ~
      (mainPrefix & path(Path.Credentials)) { routeContext: RouteContext =>
        post {
           entity(as[PasswordChangeRequest]) { request: PasswordChangeRequest =>
             authenticateBySession { userSession: UserSession =>
              complete(securityActor.ask(PasswordChange(userSession.userId, request)).mapTo[SuccessfulResponse])
             }
           }
        } ~
        get {
          parameters('verkey).as(VerificationKey) { vk: VerificationKey =>
              complete(securityActor.ask(ConfirmRegistration(vk, routeContext))(Timeout(10000)).mapTo[String])
          }
        }
      }
}

object SecurityService {

  object Path {
    val Users = PathMatcher("users")
    val Credentials = PathMatcher("credentials")
    val ResetCredentials = PathMatcher("resetcredentials")
  }

}

case class RegistrationParams(
                               @JsonProperty("email")registrationEmail: String,
                               pwd: String,
                               name: String,
                               @JsonProperty("lastname")lastName: String,
                               @JsonProperty("countrycode") countryCode: Option[String] = None,
                               phone: Option[String] = None,
                               @JsonProperty("regionid")regionId: RegionId,
                               gender: Gender,
                               birthday: Date) extends UnmarshallerEntity with Validatable {
  def email = registrationEmail.toLowerCase
  def loginParams: LoginParams = LoginParams(email, pwd)
  def registrationPhone = phone.map(p => Phone(countryCode getOrElse "", p))

  override def validate(ctx: ValidationContext) = if (countryCode.isDefined ^ phone.isDefined) {
    ctx.fail("County code and phone must be defined if you define one of them")
  }

  def phoneShort: Option[PhoneShort] =
    for {
      code <- countryCode
      number <- phone
    } yield PhoneShort(code, number)
}

case class PasswordRecoveryRequest(email: String) extends UnmarshallerEntity

case class PasswordChangeRequest(@JsonProperty("oldpwd") oldPassword: String,
                                  @JsonProperty("pwd") newPassword: String,
                                  @JsonProperty("pwdvalidation") passwordConfirmation: String) extends UnmarshallerEntity with Validatable {

  def validate(ctx: ValidationContext) {

    if(newPassword != passwordConfirmation)
      ctx.fail("Confirm password doesn't match")

  }

}