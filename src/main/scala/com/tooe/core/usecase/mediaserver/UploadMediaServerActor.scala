package com.tooe.core.usecase

import akka.pattern.pipe
import scala.concurrent.Future
import akka.actor.Actor
import com.tooe.core.application.Actors
import org.apache.http.entity.mime.content.{StringBody, FileBody}
import java.io.File
import org.apache.http.entity.mime.MultipartEntity
import com.tooe.core.util.ActorHelper
import com.tooe.core.db.mongo.util.JacksonModuleScalaSupport
import spray.json.JsString

object UploadMediaServerActor {
  final val Id = Actors.UploadMediaServer

  case class SavePhoto(uploadInfo: ImageInfo)
}

class UploadMediaServerActor extends Actor with MediaServerHelper with ActorHelper with JacksonModuleScalaSupport with MediaServerTimeout {

  import scala.concurrent.ExecutionContext.Implicits.global
  import UploadMediaServerActor._

  val uploadField = "upload_file"
  val ownerField = "ownerID"
  val mediaObjectIdField = "mediaobjectID"

  def receive = {
    case SavePhoto(uploadInfo) =>
      Future {
        val requestEntity: MultipartEntity = new MultipartEntity
        requestEntity.addPart(ownerField, new StringBody(uploadInfo.ownerId.toString))
        requestEntity.addPart(uploadField, uploadFile(uploadInfo.name))
        val jsonResponse = getMediaServerJSONResponse(requestEntity, uploadInfo.imageType)
        val JsString(photoUrl) = jsonResponse.asJsObject.fields.getOrElse(mediaObjectIdField, JsString("")) //TODO throw Exception?
        log.debug(s"upload: ${photoUrl}")
        photoUrl
      } pipeTo sender
  }

  def uploadFile(fileName: String) = new FileBody(new File(fileName))

}

@deprecated("no usages")
case class UploadAction(action: String = "upload", params: UploadParams)
@deprecated("no usages")
case class UploadParams(file: String, filetype: String, user: String, key: String)