package com.tooe.core.usecase

import com.tooe.core.db.mongo.domain._
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.application.Actors
import java.util.Date
import com.tooe.core.usecase.security.CredentialsDataActor
import com.tooe.api.service._
import com.tooe.core.exceptions.{ConflictAppException, NotFoundException}
import user.UserDataActor
import com.tooe.core.usecase.star_category.StarsCategoriesDataActor
import com.tooe.core.domain._
import scala.Some
import com.tooe.core.usecase.InfoMessageActor.GetInfoMessage
import com.tooe.core.usecase.security.CredentialsDataActor.ChangeUserPassword
import com.tooe.core.usecase.present.WelcomePresentWriteActor
import com.toiserver.core.usecase.notification.message._
import com.toiserver.core.usecase.registration.message.XMPPRegistration

object SecurityActor {
  final val Id = Actors.Security

  case class RegisterUser(params: RegistrationParams, ctx: RouteContext)
  case class RegisterStar(params: StarRegistrationParams, adminUserId: AdminUserId, ctx: RouteContext)
  case class PasswordRecovery(email: String)
  case class ConfirmRegistration(verificationKey: VerificationKey, ctx: RouteContext)
  case class PasswordChange(userId: UserId, request: PasswordChangeRequest)
}

class SecurityActor extends AppActor with ExecutionContextProvider {

  import SecurityActor._

  lazy val credentialsDataActor = lookup(CredentialsDataActor.Id)
  lazy val userWriteActor = lookup(UserWriteActor.Id)
  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val infoMessageActor = lookup(InfoMessageActor.Id)
  lazy val starsCategoriesDataActor = lookup(StarsCategoriesDataActor.Id)
  lazy val welcomePresentWriteActor = lookup(WelcomePresentWriteActor.Id)
  lazy val notificationActor = lookup('notificationActor)
  lazy val xmppAccountActor = lookup('xmppAccountActor)

  def receive = {

    case RegisterUser(params, ctx) =>
      val future = for {
        user <- (userWriteActor ? UserWriteActor.CreateNewUser(params, ctx.lang)).mapTo[User]
        credentials <- saveCredentials(user, params.email, params.pwd)
      } yield {
        notificationActor ! new RegistrationEmail(credentials.userName,
          s"/${ctx.versionId}/${ctx.langId}/credentials?verkey=${credentials.verificationKey.get}"
        )
        xmppAccountActor ! new XMPPRegistration(user.id.id.toString, credentials.passwordHash)
        RegisterUserResponse(UserDto(credentials.userId))
      }
      future pipeTo sender

    case RegisterStar(params, adminUserId, ctx) =>
      val future = for {
        user <- (userWriteActor ? UserWriteActor.CreateNewStar(params, adminUserId, ctx.lang)).mapTo[User]
        credentials <- saveCredentials(user, params.email, params.pwd)
      } yield {
        starsCategoriesDataActor ! StarsCategoriesDataActor.UpdateSubscribers(params.category, 1)
        RegisterStarResponse(UserDto(credentials.userId))
      }
      future pipeTo sender

    case ConfirmRegistration(vk, ctx) =>
      implicit val lang = ctx.lang
      credentialsDataActor.ask(CredentialsDataActor.FindByVerificationKey(vk)).mapTo[Option[Credentials]] flatMap {
        case Some(c) if c.verificationTime.isDefined =>
          throw ConflictAppException("Credentials have been already verified")

        case Some(c) =>
          val verifiedUC = c.copy(verificationTime = Option(new Date()))
          for {
            credentials <- credentialsDataActor.ask(CredentialsDataActor.Save(verifiedUC)).mapTo[Credentials]
            infoMessage <- infoMessageActor.ask(GetInfoMessage("RegistrationStatus(ok)")).mapTo[Option[InfoMessage]]
          } yield {
            welcomePresentWriteActor ! WelcomePresentWriteActor.MakeWelcomePresent(c.userId)
            userWriteActor ! UserWriteActor.IncrementUserCounter(c.userId, 1)
            infoMessage.flatMap(_.message.localized) getOrElse "successful registration"
          }

        case None => throw NotFoundException("Couldn't find credentials for such verification key")
      } pipeTo sender

    case PasswordRecovery(e) =>
      credentialsDataActor.ask(CredentialsDataActor.ReplacePassword(e)).mapTo[String].map( newPassword => {
        // TODO Return activation link instead of plain password to avoid resetting password by anyone
        notificationActor ! new NewPasswordEmail(e, newPassword)
        SuccessfulResponse
      }) pipeTo sender

    case PasswordChange(userId, request) =>
      (credentialsDataActor ? ChangeUserPassword(userId, request)).map(_ => SuccessfulResponse) pipeTo sender
  }

  def saveCredentials(user: User, email: String, pwd: String) = {
    credentialsDataActor.ask(CredentialsDataActor.Save(Credentials(user.id.id, email, pwd))).mapTo[Credentials]
  }
}

case class RegisterUserResponse(user: UserDto) extends SuccessfulResponse
case class RegisterStarResponse(star: UserDto) extends SuccessfulResponse
case class UserDto(id: UserId) extends UnmarshallerEntity