package com.tooe.api

import concurrent.{ExecutionContext, Future}
import scala.util.Try
import com.tooe.core.exceptions.AppException
import validation.ErrorMessage
import com.tooe.core.db.mongo.util.JacksonModuleScalaSupport
import scala.reflect.ClassTag
import spray.httpx.marshalling.Marshaller
import spray.httpx.unmarshalling.{MalformedContent, DeserializationError, Deserializer}
import org.bson.types.ObjectId
import scala.util.control.NonFatal
import com.tooe.core.domain._
import java.util.Date
import com.tooe.core.domain.UserId
import com.tooe.core.domain.PromotionId
import spray.routing.RequestContext
import util.Failure
import com.tooe.core.domain.CurrencyId
import com.tooe.core.domain.RegionId

package object service {

  implicit class FutureWrapper[T : Marshaller : ClassTag](val future: Future[T]) extends JacksonModuleScalaSupport {

    @deprecated("use standard complete method since AppExceptions are handled by standard Spray error handling")
    def onCompleteEx[U](customHandler: PartialFunction[Try[T], U])(implicit executor: ExecutionContext, ctx: RequestContext): Unit =
      future.onComplete(customHandler orElse failHandler)

    private def failHandler(implicit executor: ExecutionContext, ctx: RequestContext): PartialFunction[Try[T], Unit] = {
      case Failure(ex: AppException) =>
        ctx.complete(ex.statusCode, FailureResponse(ErrorMessage(ex.message, ex.errorCode)))
      case Failure(ex) =>
        ex.printStackTrace()
        ctx.complete(500, s"$ex\n${ex.getStackTraceString}")
    }
  }

  implicit val string2UsersGroupId: Deserializer[String, UsersGroupId] = customDeserializer(value => UsersGroupId(value))
  implicit val string2LocationsChainId: Deserializer[String, LocationsChainId] = customDeserializer(value => LocationsChainId(new ObjectId(value)))
  implicit val string2UserCommentId: Deserializer[String, UserCommentId] = customDeserializer(value => UserCommentId(new ObjectId(value)))
  implicit val string2VerificationKey: Deserializer[String, VerificationKey] = customDeserializer(value => VerificationKey(value))
  implicit val string2ObjectId: Deserializer[String, ObjectId] = customDeserializer(value => new ObjectId(value))
  implicit val string2UserId: Deserializer[String, UserId] = customDeserializer(value => UserId(new ObjectId(value)))
  implicit val string2NewsId: Deserializer[String, NewsId] = customDeserializer(value => NewsId(new ObjectId(value)))
  implicit val string2RegionId: Deserializer[String, RegionId] = customDeserializer(value => RegionId(new ObjectId(value)))
  implicit val string2CountryId: Deserializer[String, CountryId] = customDeserializer(value => CountryId(value))
  implicit val string2LocationId: Deserializer[String, LocationId] = customDeserializer(value => LocationId(new ObjectId(value)))
  implicit val string2CurrencyId: Deserializer[String, CurrencyId] = customDeserializer(value => CurrencyId(value))
  implicit val string2Date: Deserializer[String, Date] = customDeserializer( value => new Date(value.toLong * 1000) )
  implicit val string2PromotionId: Deserializer[String, PromotionId] = customDeserializer(value => PromotionId(new ObjectId(value)))
  implicit val string2PhotoAlbumId: Deserializer[String, PhotoAlbumId] = customDeserializer(value => PhotoAlbumId(new ObjectId(value)))
  implicit val string2LocationCategoryId: Deserializer[String, LocationCategoryId] = customDeserializer(value => LocationCategoryId(value))
  implicit val string2AdditionalLocationCategoryId: Deserializer[String, AdditionalLocationCategoryId] = customDeserializer(value => AdditionalLocationCategoryId(new ObjectId(value)))
  implicit val string2StarCategoryId: Deserializer[String, StarCategoryId] = customDeserializer(value => StarCategoryId(value))
  implicit val string2ModerationStatusId: Deserializer[String, ModerationStatusId] = customDeserializer(value => ModerationStatusId(value))
  implicit val string2PresentStatusId: Deserializer[String, PresentStatusId] = customDeserializer(value => PresentStatusId(value))
  implicit val string2ProductTypeId: Deserializer[String, ProductTypeId] = customDeserializer(value => ProductTypeId(value))
  implicit val string2CompanyId: Deserializer[String, CompanyId] = customDeserializer(value => CompanyId(new ObjectId(value)))

  def customDeserializer[T : ClassTag, B : ClassTag](fun: B => T): Deserializer[B, T] = new Deserializer[B, T] {
    def apply(value: B): Either[DeserializationError, T] =
      try  Right(fun(value))
      catch {
        case NonFatal(ex) =>  Left(MalformedContent("'" + value + "' is not a valid " + reflect.classTag[T].runtimeClass.getSimpleName + " value"))
      }
  }
  object ObjectId extends (String => ObjectId) {
    def apply(s: String) = new ObjectId(s)
    def apply() = new ObjectId()
  }

}