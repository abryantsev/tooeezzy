package com.tooe.core.usecase.news

import akka.actor.ActorSystem
import com.tooe.core.domain.MediaUrl
import com.tooe.core.util.MediaHelper

class AnonymousNewsActorSettings(implicit system: ActorSystem) {
  private val config = system.settings.config.getConfig("images")
  private object AnonymousAvatar {
    val urn = config.getString("default_images_names.user.media.m")
    val size = config.getString("news.full.actor.media")
  }

  val mediaUrl: MediaUrl = MediaHelper.staticMediaUrl(AnonymousAvatar.urn, AnonymousAvatar.size)
}