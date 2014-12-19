package com.tooe.core.usecase

import akka.actor.Actor
import com.tooe.core.db.mongo.util.{UnmarshallerEntity, JacksonModuleScalaSupport}
import com.tooe.core.util.ActorHelper
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.application.Actors
import org.bson.types.ObjectId
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.StringBody

object CreateLinkMediaServerActor {
  final val Id = Actors.CreateLinkMediaServer

  //TODO: imageType must be ImageType.Value after test
  case class CreateLink(target: String, source: ObjectId, imageType: String)

}

class CreateLinkMediaServerActor extends Actor with JacksonModuleScalaSupport with MediaServerHelper with ActorHelper with DefaultTimeout {

  import CreateLinkMediaServerActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  def receive = {

    case CreateLink(target, source, imageType) =>
     /* val requestEntity: MultipartEntity = new MultipartEntity
      requestEntity.addRequest(new StringBody(buildRequest(target, source, imageType)))
      getMediaServerResponse(requestEntity)*/
  }

}

case class CreateLinkRequest(target: String, source: ObjectId, folder: String) extends UnmarshallerEntity

case class CreateLinkAction(action: String = "link", params: CreateLinkParams)

case class CreateLinkParams(folder: String, @JsonProperty("trg-id") target: String, @JsonProperty("src-id") source: String)