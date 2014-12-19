package com.tooe.api.service

import akka.actor.ActorSystem
import spray.routing._
import com.tooe.api.marshalling.Marshalling
import spray.httpx.marshalling.{BasicMarshallers, Marshaller, MetaMarshallers}
import com.tooe.api.boot.DefaultTimeout
import spray.http._
import com.tooe.core.db.mongo.util.JacksonModuleScalaSupport
import spray.http.MediaTypes._
import com.tooe.core.application.AppActors
import scala.concurrent.Future
import com.tooe.core.usecase.{AuthorizationActor, AdminSessionActor, SessionActor}
import akka.pattern.ask
import com.tooe.core.usecase.SessionActor.AuthResult
import spray.http.Uri.Path
import spray.routing.PathMatcher.{Unmatched, Matched}
import shapeless.HNil
import com.tooe.core.exceptions.BadRequestException
import scala.collection.JavaConverters._
import com.tooe.core.util.Lang

abstract class SprayServiceBaseClass2(implicit system: ActorSystem)
  extends SprayServiceBaseHelper
  with AppAuthHelper
  with AppActors
  with OffsetLimitHelper
  with CoordinatesHelper
{
  private lazy val sessionActor = lookup(SessionActor.Id)
  private lazy val adminSessionActor = lookup(AdminSessionActor.Id)
  private lazy val authorizationActor = lookup(AuthorizationActor.Id)

  val allowedApiVersions: Set[String] =
    system.settings.config.getStringList("api.allowed-versions").asScala.toSet

  override def filterApiVersion(versionId: String): Boolean = allowedApiVersions contains versionId

  def getAuthResult(authCookies: AuthCookies) =
    (sessionActor ? SessionActor.Authenticate(authCookies)).mapTo[AuthResult]

  def getAdminAuthResult(authCookies: AuthCookies): Future[AdminSessionActor.AuthResult] =
    (adminSessionActor ? AdminSessionActor.Authenticate(authCookies)).mapTo[AdminSessionActor.AuthResult]

  protected def checkResourceAccess(session: AdminUserSession, resourceId: ResourceId) =
    (authorizationActor ? AuthorizationActor.CheckResourceAccess(session.adminUserId, session.role, session.companies, resourceId)).mapTo[Boolean]
}

trait ExecutionContextProvider {
  implicit val ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
}

trait SprayServiceBaseHelper
  extends Directives
  with Marshalling
  with MetaMarshallers
  with BasicMarshallers
  with DefaultTimeout
  with JacksonModuleScalaSupport
  with Utf8EncodingContentTypeHelper
  with AppActors
  with MainPrefixHelper

trait MainPrefixHelper { self: Directives =>

  def filterApiVersion(versionId: String): Boolean = true

  private def deserializeApiVersion(versionId: String): ApiVersion = ApiVersion(versionId)

  private class ApiVersionMatcher(magnet: ApiVersionMagnet) extends PathMatcher1[ApiVersion] {

    def apply(path: Path) = path match {
      case Path.Segment(segment, tail) ⇒
        if (!filterApiVersion(segment)) {
          throw BadRequestException(s"Not allowed API version '$segment'.")
        }
        val version = deserializeApiVersion(segment)
        if (magnet.checkMatching(version)) Matched(tail, version :: HNil)
        else Unmatched
      case _ ⇒ Unmatched
    }
  }

  trait ApiVersionMagnet {
    def checkMatching(version: ApiVersion): Boolean
  }

  object ApiVersionMagnet {
    val AlwaysPass = new ApiVersionMagnet {
      def checkMatching(version: ApiVersion) = true
    }

    implicit def fromSet(versionSet: Set[ApiVersion]) = new ApiVersionMagnet {
      def checkMatching(version: ApiVersion) = versionSet contains version
    }

    implicit def fromOne(allowedVersion: ApiVersion) = new ApiVersionMagnet {
      def checkMatching(version: ApiVersion) = allowedVersion == version
    }
  }

  private object LangMatcher extends PathMatcher1[Lang] {
    def apply(path: Path) = path match {
      case Path.Segment(segment, tail) ⇒ Matched(tail, Lang(segment) :: HNil)
      case _                           ⇒ Unmatched
    }
  }

  def mainPrefix(magnet: ApiVersionMagnet): Directive1[RouteContext] =
    pathPrefix(new ApiVersionMatcher(magnet) / LangMatcher) as VersionLang

  val mainPrefix: Directive1[RouteContext] = mainPrefix(ApiVersionMagnet.AlwaysPass)
}

trait Utf8EncodingContentTypeHelper {
  def toUtf8ContentType(mediaType: MediaType): ContentType = ContentType(mediaType, HttpCharsets.`UTF-8`)

  implicit val Utf8FormDataMarshaller =
    Marshaller.delegate[FormData, String](toUtf8ContentType(`application/x-www-form-urlencoded`)) { (formData, contentType) =>
      import java.net.URLEncoder.encode
      val charset = contentType.charset.value
      formData.fields.map { case (key, value) => encode(key, charset) + '=' + encode(value, charset) }.mkString("&")
    }

  object ObjectId extends PathMatcher1[org.bson.types.ObjectId] {
    val Matcher = """^([0-9a-fA-F]{24})$""".r

    def apply(path: Path) = path match {
      case Path.Segment(Matcher(segment), tail) => Matched(tail, new org.bson.types.ObjectId(segment) :: HNil)
      case _                                    => Unmatched
    }
  }
}