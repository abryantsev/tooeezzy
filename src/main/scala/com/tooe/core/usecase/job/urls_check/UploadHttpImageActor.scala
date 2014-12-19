package com.tooe.core.usecase.job.urls_check

import com.tooe.core.application.Actors
import com.tooe.core.usecase.AppActor
import com.tooe.core.usecase.urls.UrlsDataActor
import akka.io.IO
import spray.can.Http
import spray.httpx.RequestBuilding._
import com.tooe.core.db.mongo.domain.Urls
import spray.http.{StatusCodes, HttpResponse, Uri}
import spray.json._
import com.tooe.core.domain.{UrlsId, UrlType, MediaObjectId}
import org.bson.types.ObjectId
import org.joda.time.DateTime

object UploadHttpImageActor {
  val Id = Actors.uploadHttpImage

  case class UploadImage(url: Urls)
}

class UploadHttpImageActor extends AppActor with MediaUrlJobLogger {
  import scala.concurrent.ExecutionContext.Implicits.global
  import UploadHttpImageActor._

  override implicit val timeout = settings.Job.UrlCheck.TIMEOUT

  lazy val urlDataActor = lookup(UrlsDataActor.Id)

  val mediaObjectIdField = "mediaobjectID"

  lazy val urlTypeChangeActor = lookup(UrlTypeChangeActor.Id)

  def receive = {
    case UploadImage(url) =>
      val mediaServerType = getMediaServerSuffixByEntityType(url)
      implicit val system = context.system
      val uploadUrl = migrationUrl(mediaServerType, url.entityId)
      debug(s"upload to $uploadUrl with body: ${url.mediaId.id}")
      for {
        httpResponse <- IO(Http).ask(Post(Uri(uploadUrl), url.mediaId.id)).mapTo[HttpResponse]
      } yield {
        debug(s"upload to $uploadUrl with body: ${url.mediaId.id} has status: ${httpResponse.status.value}")
        if (httpResponse.status == StatusCodes.OK) {
          val response = httpResponse.entity.asString.asJson
          response.asJsObject.fields.get(mediaObjectIdField) match {
            case Some(JsString(mediaUrl)) =>
              debug(s"returned media object ${mediaUrl} for ${url.entityId.toString}")
              urlDataActor ! UrlsDataActor.DeleteUrls(url.id)
              val newUrls = url.copy(mediaId = MediaObjectId(mediaUrl), urlType = Some(UrlType.s3), time = DateTime.now().plusMinutes(10).toDate, id = UrlsId())
              urlDataActor ! UrlsDataActor.SaveUrls(newUrls)
              debug(s"replace ${url.id.id} with ${newUrls.id.id}")
              urlTypeChangeActor ! ChangeUrlType.ChangeTypeToS3(url, MediaObjectId(mediaUrl))
            case _ => log.warning("cannot upload {} to media server", url.mediaId.id)
          }
        }
      }
  }

  def migrationUrl(imageType: String, ownerId: ObjectId) =
    s"${settings.MigrationMediaServer.Host}?migrator=${imageType}&ownerID=${ownerId.toString}"

}
