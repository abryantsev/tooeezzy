package com.tooe.core.usecase.job.urls_check

import akka.actor.ActorLogging
import com.tooe.core.usecase.AppActor

import org.slf4j.LoggerFactory
import akka.event.Logging
import org.slf4j.Logger 

trait MediaUrlJobLogger extends ActorLogging { self: AppActor =>

  lazy val logger = LoggerFactory.getLogger(getClass)
  private val show = settings.Job.UrlCheck.showLog

  def debug(msg: String) {
    if(show)
      logger.debug(msg)
  }

  def warn(msg: String, attr: String) {
    if(show)
      logger.warn(msg, attr)
  }  
}
