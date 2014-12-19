package com.tooe.core.usecase

import akka.actor.Actor
import com.tooe.core.db.mongo.util.JacksonModuleScalaSupport
import com.tooe.core.util.ActorHelper
import com.tooe.core.application.Actors
import com.tooe.api.boot.DefaultTimeout
import akka.io.IO
import spray.can.Http
import spray.httpx.RequestBuilding._
import spray.http.{StatusCodes, HttpResponse, Uri}
import akka.pattern._
import spray.json._

object DeleteMediaServerActor {
  final val Id = Actors.DeleteMediaServer

  case class DeletePhotoFile(images: Seq[ImageInfo])

}

class DeleteMediaServerActor extends Actor with JacksonModuleScalaSupport with MediaServerHelper with ActorHelper with DefaultTimeout{

  import scala.concurrent.ExecutionContext.Implicits.global
  import DeleteMediaServerActor._

  implicit val system = context.system

  val metaField = "meta"
  val statusField = "status"

  def receive = {

    case DeletePhotoFile(images) =>
      images.foreach { image =>
        val imageDeleteUrl = s"${getHostUrl(image.imageType)}?mediaobjectID=${image.name}"
        IO(Http).ask(Delete(Uri(imageDeleteUrl))).mapTo[HttpResponse].map { response =>
          if(response.status == StatusCodes.OK) {
            val jsonResult = response.entity.asString.asJson
            val success = jsonResult.asJsObject.fields(metaField).asJsObject.fields(statusField) == JsString("success")
            if(!success)
              log.error(s"cannot delete: ${image.name}")
            else
              log.debug(s"success delete: ${image.name}")
          }
          else log.error(response.entity.asString)
        }
      }
  }

}