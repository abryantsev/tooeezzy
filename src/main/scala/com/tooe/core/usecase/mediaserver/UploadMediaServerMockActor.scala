package com.tooe.core.usecase.mediaserver

import akka.actor.Actor
import com.tooe.core.usecase.UploadMediaServerActor

class UploadMediaServerMockActor extends Actor {

  import UploadMediaServerActor._

  def receive = {
    case SavePhoto(uploadInfo) =>
      val url = uploadInfo.name
      sender ! "mock-server-url" + url
  }
}