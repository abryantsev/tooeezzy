package com.tooe.core.usecase

import com.tooe.api.service.ExecutionContextProvider
import com.tooe.core.application.Actors
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain._
import scala.concurrent.Future
import com.tooe.core.usecase.location.{PreModerationLocationDataActor, LocationDataActor}
import com.tooe.core.usecase.product.ProductDataActor
import com.tooe.core.usecase.company.PreModerationCompanyDataActor
import com.tooe.core.exceptions.ForbiddenAppException
import com.tooe.core.usecase.AuthorizationActor.CheckResourceAccess
import com.tooe.core.usecase.company.PreModerationCompanyDataActor.FindCompany
import com.tooe.core.usecase.present.PresentDataActor
import com.tooe.core.usecase.location_news.LocationNewsDataActor
import com.tooe.core.usecase.location_photoalbum.LocationPhotoAlbumDataActor
import com.tooe.core.usecase.location_photo.LocationPhotoDataActor

object AuthorizationActor {
  final val Id = Actors.AuthorizationActor

  case class CheckResourceAccess(adminUserId: AdminUserId, role: AdminRoleId, companies: Set[CompanyId], resourceId: ObjectiveId)

}

class AuthorizationActor extends AppActor with ExecutionContextProvider {

  import AdminRoleId._

  type AuthorizationCase = PartialFunction[ObjectiveId, Future[Boolean]]

  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val productDataActor = lookup(ProductDataActor.Id)
  lazy val presentDataActor = lookup(PresentDataActor.Id)
  lazy val preModerationCompanyDataActor = lookup(PreModerationCompanyDataActor.Id)
  lazy val preModerationLocationDataActor = lookup(PreModerationLocationDataActor.Id)
  lazy val locationNewsDataActor = lookup(LocationNewsDataActor.Id)
  lazy val locationPhotoAlbumDataActor = lookup(LocationPhotoAlbumDataActor.Id)
  lazy val locationPhotoDataActor = lookup(LocationPhotoDataActor.Id)

  def receive = {
    case msg@CheckResourceAccess(adminUserId, role, companies, resourceId) =>
      val future = authCases(msg).applyOrElse[ObjectiveId, Future[Boolean]](resourceId, _ => Future.failed(ForbiddenAppException("Access denied")))
      future onSuccess {
        case r => logCheck(msg)(r)
      }
      future pipeTo sender
  }

  def authCases(msg: CheckResourceAccess) =
    defaultAuthCases(msg.adminUserId, msg.role, msg.companies) orElse
      presentAuthCases(msg.role, msg.companies) orElse
      locationAuthCases(msg.role, msg.companies)

  def defaultAuthCases(adminUserId: AdminUserId, role: AdminRoleId, companies: Set[CompanyId]): AuthorizationCase = {
    case id: CompanyId => Future successful (companies contains id)
    case id: ProductId => getProduct(id) map (companies contains _.companyId)
    case id: PreModerationCompanyId => getCompanyMod(id) map (_.agentId == adminUserId)
    case id: LocationNewsId => getLocationNews(id) flatMap (news => getLocationWithAnyLifeCycleStatus(news.locationId) map (companies contains _.companyId))
    case id: LocationPhotoAlbumId => getLocationPhotoAlbum(id) flatMap (album => getLocationWithAnyLifeCycleStatus(album.locationId) map (companies contains _.companyId))
    case id: LocationPhotoId => for {
        photo <- getLocationPhoto(id)
        album <- getLocationPhotoAlbum(photo.photoAlbumId)
        location <- getLocation(album.locationId)
      } yield companies contains location.companyId
  }

  def locationAuthCases(role: AdminRoleId, companies: Set[CompanyId]): AuthorizationCase = role match {
    case Admin | Moderator => {
      case id: LocationId => Future.successful(true)
      case id: PreModerationLocationId => Future.successful(true)
    }
    case Client => {
      case id: LocationId => getLocationWithAnyLifeCycleStatus(id) map (companies contains _.companyId)
      case id: PreModerationLocationId => getLocationMod(id) map (companies contains _.companyId)
    }
    case _ => undefined
  }

  def presentAuthCases(role: AdminRoleId, companies: Set[CompanyId]): AuthorizationCase = role match {
    case Activator => {
      case id: PresentId if companies.isEmpty => Future.successful(true)
      case id: PresentId => getPresent(id) map (companies contains _.product.companyId)
      case code: PresentCode if companies.isEmpty => Future.successful(true)
      case code: PresentCode => getPresentByCode(code) map (companies contains _.product.companyId)
    }
    case Client => {
      case id: PresentId => getPresent(id) map (companies contains _.product.companyId)
      case code: PresentCode => getPresentByCode(code) map (companies contains _.product.companyId)
    }
    case _ => undefined
  }

  val undefined: AuthorizationCase = Map.empty

  def logCheck(msg: CheckResourceAccess)(checkResult: Boolean): Unit =
    if (!checkResult) {
      log.info("Access denied " + msg)
    }

  def getPresentByCode(code: PresentCode): Future[Present] =
    presentDataActor.ask(PresentDataActor.GetUserPresentByCode(code)).mapTo[Present]

  def getPresent(id: PresentId): Future[Present] =
    presentDataActor.ask(PresentDataActor.GetPresent(id)).mapTo[Present]

  def getLocation(id: LocationId): Future[Location] =
    (locationDataActor ? LocationDataActor.GetLocation(id)).mapTo[Location]

  def getLocationWithAnyLifeCycleStatus(id: LocationId): Future[Location] =
    (locationDataActor ? LocationDataActor.GetLocationWithAnyLifeCycleStatus(id)).mapTo[Location]

  def getProduct(id: ProductId): Future[Product] = (productDataActor ? ProductDataActor.GetProduct(id)).mapTo[Product]

  def getCompanyMod(id: PreModerationCompanyId): Future[PreModerationCompany] =
    (preModerationCompanyDataActor ? FindCompany(id)).mapTo[PreModerationCompany]

  def getLocationMod(id: PreModerationLocationId): Future[PreModerationLocation] =
    (preModerationLocationDataActor ? PreModerationLocationDataActor.FindLocationById(id)).mapTo[PreModerationLocation]

  def getLocationNews(id: LocationNewsId): Future[LocationNews] =
    (locationNewsDataActor ? LocationNewsDataActor.FindLocationNews(id)).mapTo[LocationNews]

  def getLocationPhotoAlbum(id: LocationPhotoAlbumId): Future[LocationPhotoAlbum] =
    (locationPhotoAlbumDataActor ? LocationPhotoAlbumDataActor.FindLocationPhotoAlbum(id)).mapTo[LocationPhotoAlbum]

  def getLocationPhoto(id: LocationPhotoId): Future[LocationPhoto] =
    (locationPhotoDataActor ? LocationPhotoDataActor.FindLocationPhoto(id)).mapTo[LocationPhoto]
}