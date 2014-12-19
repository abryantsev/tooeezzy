package com.tooe.core.usecase.company

import com.tooe.core.application.Actors
import com.tooe.core.usecase._
import com.tooe.core.domain._
import com.tooe.api.service._
import com.tooe.core.usecase.admin_user.{AdminCredentialsDataActor, AdminUserDataActor}
import com.tooe.core.usecase.admin_user_event.AdminUserEventDataActor
import java.util.Date
import com.tooe.core.usecase.urls.{UrlsDataActor, UrlsWriteActor}
import com.fasterxml.jackson.annotation.JsonProperty
import scala.concurrent.Future
import com.tooe.core.db.mongo.domain._
import com.tooe.core.usecase.InfoMessageActor.GetMessage
import com.tooe.core.usecase.admin_user_event.AdminUserEventDataActor.SaveAdminEvent
import com.tooe.core.usecase.admin_user.AdminUserDataActor.ActivateAdminUser
import scala.Some
import com.tooe.core.usecase.company.PreModerationCompanyDataActor._
import com.tooe.core.usecase.DeleteMediaServerActor.DeletePhotoFile
import com.tooe.core.domain.PreModerationCompanyId
import com.tooe.core.usecase.admin_user.AdminUserDataActor.SaveAdminUser
import com.tooe.core.exceptions.ApplicationException
import com.tooe.core.usecase.admin_user.AdminCredentialsDataActor.SaveAdminCredentials
import com.tooe.core.util.{Lang, InfoMessageHelper, HashHelper}
import com.tooe.core.db.mongo.query.UpdateResult

object ModerationCompanyWriteActor {
  final val Id = Actors.ModerationCompanyWriteActor

  case class UpdateModerationStatus(companyId: PreModerationCompanyId, request: CompanyModerationRequest, userId: AdminUserId, ctx: RouteContext)

  case class UpdateCompanyMedia(companyId: PreModerationCompanyId, mediaId: MediaObjectId)

  case class CreateCompany(request: CompanyCreateRequest)

  case class ChangeCompany(companyId: PreModerationCompanyId, request: CompanyChangeRequest, lang: Lang)

}

class ModerationCompanyWriteActor extends AppActor {

  import ModerationCompanyWriteActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val preModerationCompanyDataActor = lookup(PreModerationCompanyDataActor.Id)
  lazy val companyDataActor = lookup(CompanyDataActor.Id)
  lazy val adminUserDataActor = lookup(AdminUserDataActor.Id)
  lazy val adminUserEventDataActor = lookup(AdminUserEventDataActor.Id)
  lazy val infoMessageActor = lookup(InfoMessageActor.Id)
  lazy val adminUserWriteActor = lookup(AdminUserDataActor.Id)
  lazy val adminCredentialsDataActor = lookup(AdminCredentialsDataActor.Id)
  lazy val cacheAdminSessionDataActor = lookup(CacheAdminSessionDataActor.Id)
  lazy val urlsWriteActor = lookup(UrlsWriteActor.Id)
  lazy val deleteMediaServerActor = lookup(DeleteMediaServerActor.Id)
  lazy val urlsDataActor = lookup(UrlsDataActor.Id)

  lazy val notificationMessageCode = "company_changes_approved_by_moderator"

  def receive = {
    case CreateCompany(request) =>
      val companyId = PreModerationCompanyId()
      val userId = AdminUserId()
      (for {
        _ <- isEmailAlreadyExists(request.user.email).map(p => if (p) throw new ApplicationException(message = "Admin user with this e-mail already exists.") else p)
        (adminUser, adminUserCredential) <- (adminUserWriteActor ? SaveAdminUser(getUserFromRequest(request.user, userId)))
          .zip(adminCredentialsDataActor ? SaveAdminCredentials(getCredentialFromRequest(request.user, userId))).mapTo[(AdminUser, AdminCredentials)]
        company <- (preModerationCompanyDataActor ? SaveCompany(requestToCompany(request, userId, companyId))).mapTo[PreModerationCompany]
      } yield {
        addModerationCompanyUrl(company.id, company.companyMedia.map(_.url.url))
        CreateCompanyResponse(company) //TODO discus about return value
      }) pipeTo sender

    case UpdateModerationStatus(companyId, request, userId, ctx) =>
      request.status match {
        case ModerationStatusId.Active =>
          (for {
            moderatedCompany <- getCompanyMod(companyId)
            oldCompanyMedia <- getCompanyMedia(moderatedCompany.publishCompany)
            company <- (companyDataActor ? CompanyDataActor.SaveCompany(moderationCompanyToCompany(moderatedCompany))).mapTo[Company]
          } yield {
            gradPermissionToCompany(company.id, company.agentId)
            updateUserLifeCycleStatus(moderatedCompany, company.id)
            oldCompanyMedia.foreach {
              media =>
                val mediaForDelete = media.toSet -- company.companyMedia.map(_.url.url).toSet
                deleteMedia(mediaForDelete.map(m => ImageInfo(m.id, ImageType.company, company.id.id)).toSeq :_*)
                urlsDataActor ! UrlsDataActor.DeleteUrlsByEntityAndUrl(mediaForDelete.map(media => company.id.id -> media).toSeq)
            }
            updateModerationStatus(companyId, request, userId, if (moderatedCompany.publishCompany.isEmpty) Some(company.id) else None)
            (infoMessageActor ? GetMessage(notificationMessageCode, ctx.lang)).mapTo[String].map {
              message: String =>
                findClientsAndAgents(company.id).foreach(_.foreach(admin =>
                  adminUserEventDataActor ! SaveAdminEvent(AdminUserEvent(adminUserId = admin.id, message = message))
                ))
            }
            urlsWriteActor ! UrlsWriteActor.AddCompanyUrl(company.id, company.companyMedia.map(_.url.url))
            CompanyModerationUpdateResponse(Some(CompanyPublishId(company.id)))
          }) pipeTo sender
        case _ =>
          (preModerationCompanyDataActor ? GetPublishCompanyId(companyId)).mapTo[Option[CompanyId]] map {
            publishCompanyId =>
              updateModerationStatus(companyId, request, userId)
              CompanyModerationUpdateResponse(publishCompanyId.map(CompanyPublishId))
          } pipeTo sender
      }

    case ChangeCompany(id, request, lang) =>
      getCompanyMod(id).flatMap {
        case c if c.moderationStatus.status == ModerationStatusId.Waiting =>
          InfoMessageHelper.throwAppExceptionById("update_company_with_waiting_status")(lang)
        case _ =>
          preModerationCompanyDataActor ! PreModerationCompanyDataActor.ChangeCompany(id, request)
          request.media.foreach {
            media =>
              addModerationCompanyUrl(id, Seq(MediaObjectId(media)))
          }
          Future.successful(SuccessfulResponse)
      }.pipeTo(sender)

    case UpdateCompanyMedia(id, mediaId) =>
      val result = for {
        company <- getCompanyMod(id)
        media = CompanyMedia(MediaObject(mediaId), Some("main"))
        result <- updateCompanyMedia(id, media)
      } yield result match {
          case UpdateResult.Updated =>
            if (company.companyMedia.nonEmpty) {
              deleteMedia(company.companyMedia.map(cm => ImageInfo(cm.url.url.id, ImageType.company, id.id)): _*)
              urlsDataActor ! UrlsDataActor.DeleteUrlsByEntityAndUrl(company.companyMedia.map(m => id.id -> m.url.url))
            }
            addModerationCompanyUrl(id, Seq(mediaId))
            SuccessfulResponse
          case _ =>
            throw ApplicationException(message = "Company media has not been updated")
        }
      result.pipeTo(sender)
  }

  def getCompanyMod(companyId: PreModerationCompanyId) =
    preModerationCompanyDataActor.ask(FindCompany(companyId)).mapTo[PreModerationCompany]

  def updateCompanyMedia(companyId: PreModerationCompanyId, media: CompanyMedia) =
    preModerationCompanyDataActor.ask(PreModerationCompanyDataActor.UpdateCompanyMedia(companyId, media)).mapTo[UpdateResult]

  def deleteMedia(imageInfo: ImageInfo*)=
    deleteMediaServerActor ! DeletePhotoFile(imageInfo)

  def findClientsAndAgents(id: CompanyId) =
    adminUserDataActor.ask(AdminUserDataActor.FindByRolesForCompany(id, Seq(AdminRoleId.Client, AdminRoleId.Agent))).mapTo[Seq[AdminUser]]

  def getCompanyMedia(companyId: Option[CompanyId]): Future[Option[Seq[MediaObjectId]]] = {
    companyId.map {
      id =>
        (companyDataActor ? CompanyDataActor.GetCompanyMedia(id)).mapTo[Option[Seq[MediaObjectId]]]
    } getOrElse Future.successful(None)
  }

  def gradPermissionToCompany(companyId: CompanyId, userId: AdminUserId) =
    cacheAdminSessionDataActor ! CacheAdminSessionDataActor.AddCompany(userId, companyId)

  def updateModerationStatus(companyId: PreModerationCompanyId, request: CompanyModerationRequest, userId: AdminUserId, publishId: Option[CompanyId] = None) =
    preModerationCompanyDataActor ! UpdateStatus(companyId, request, userId, publishId)

  def updateUserLifeCycleStatus(moderatedCompany: PreModerationCompany, companyId: CompanyId) {
    if (moderatedCompany.publishCompany.isEmpty) {
      adminUserDataActor ! ActivateAdminUser(moderatedCompany.mainUserId, companyId)
    }
  }

  def moderationCompanyToCompany(moderatedCompany: PreModerationCompany): Company =
    Company(
      id = moderatedCompany.publishCompany.getOrElse(CompanyId()),
      agentId = moderatedCompany.agentId,
      mainUserId = moderatedCompany.mainUserId,
      registrationDate = moderatedCompany.registrationDate,
      name = moderatedCompany.name,
      shortName = moderatedCompany.shortName,
      description = moderatedCompany.description,
      subject = moderatedCompany.subject,
      contract = moderatedCompany.contract,
      contact = moderatedCompany.contact,
      management = moderatedCompany.management,
      legalInformation = moderatedCompany.legalInformation,
      companyMedia = moderatedCompany.companyMedia,
      payment = moderatedCompany.payment,
      partnershipType = moderatedCompany.partnershipType,
      updateTime = Some(new Date),
      synchronizationTime = None
    )

  def requestToCompany(request: CompanyCreateRequest, userId: AdminUserId, companyId: PreModerationCompanyId): PreModerationCompany =
    PreModerationCompany(
      id = companyId,
      agentId = request.agentId,
      mainUserId = userId,
      name = request.companyName,
      shortName = request.shortName,
      description = request.description,
      subject = request.legalInfo.subject,
      contract = CompanyContract(request.contractNumber, request.contractDate),
      contact = CompanyContact(phones = Seq(Phone(request.countryCode, request.phone, Some("main"))),
        address = request.address,
        legalAddress = request.legalInfo.legalAddress,
        url = request.url),
      management = CompanyManagement(request.management.office, request.management.manager, request.management.accountant),
      legalInformation = CompanyInformation(request.legalInfo.structure,
        request.legalInfo.ogrn,
        request.legalInfo.registrationDate,
        request.legalInfo.registrator,
        request.legalInfo.taxNumber,
        request.legalInfo.kpp,
        request.legalInfo.licence,
        request.legalType),
      companyMedia = request.media.map(media => Seq(CompanyMedia(MediaObject(media), Some("main")))).getOrElse(Nil),
      payment = CompanyPayment(request.payment.bankAccount,
        request.payment.bankIndex,
        request.payment.bankTransferAccount,
        request.payment.period,
        request.payment.limit.map(l => PaymentLimit(l.value, l.currency))),
      partnershipType = request.partnershipType
    )

  def getUserFromRequest(user: MainUser, userId: AdminUserId): AdminUser =
    AdminUser(
      id = userId,
      name = user.name,
      lastName = user.lastName,
      role = user.role.getOrElse(AdminRoleId.Client),
      companyId = None,
      lifecycleStatus = Some(LifecycleStatusId.Deactivated)
    )

  def getCredentialFromRequest(user: MainUser, userId: AdminUserId): AdminCredentials =
    AdminCredentials(
      adminUserId = userId,
      userName = user.email.toLowerCase,
      password = HashHelper.passwordHash(user.password)
    )

  def addModerationCompanyUrl(companyId: PreModerationCompanyId, urls: Seq[MediaObjectId]) {
    urlsWriteActor ! UrlsWriteActor.AddModerationCompanyUrl(companyId, urls)
  }

  def isEmailAlreadyExists(email: String) =
    (adminCredentialsDataActor ? AdminCredentialsDataActor.AdminUserEmailExist(email)).mapTo[Boolean]

}

case class CompanyModerationUpdateResponse(company: Option[CompanyPublishId]) extends SuccessfulResponse

case class CompanyPublishId(id: CompanyId)

case class CreateCompanyResponse(@JsonProperty("companiesmoderation") company: CompanyIdCreated) extends SuccessfulResponse

case class CompanyIdCreated(id: PreModerationCompanyId)

object CreateCompanyResponse {

  def apply(company: PreModerationCompany): CreateCompanyResponse = CreateCompanyResponse(CompanyIdCreated(company.id))

}