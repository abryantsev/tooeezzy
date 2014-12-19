package com.tooe.core.usecase.job.urls_check

import com.tooe.core.application.Actors
import com.tooe.core.db.mongo.domain.Urls
import com.tooe.core.usecase.AppActor
import scala.concurrent.Future
import com.tooe.core.util.MediaHelper
import com.tooe.core.usecase.urls.UrlsDataActor

object CdnUrlCheckerActor {

  val Id = Actors.s3UrlChecker

  case class UrlCheck(url: Urls)

}

class CdnUrlCheckerActor extends AppActor with MediaUrlJobLogger {

  import scala.concurrent.ExecutionContext.Implicits.global
  import CdnUrlCheckerActor._

  lazy val pingCdnUrl = lookup(PingCdnUrlActor.Id)
  lazy val urlDataActor = lookup(UrlsDataActor.Id)
  lazy val urlTypeChangeActor = lookup(UrlTypeChangeActor.Id)

  def receive = {
    case UrlCheck(url) =>
      debug(s"check urls record: ${url.id.id}")
      val urlsPing = loadAllUrls(url).map(u => pingCdnUrl ? PingCdnUrlActor.PingUrl(u))
      Future.sequence(urlsPing).mapTo[Seq[Boolean]].map { results =>
        if(results.reduce(_ && _)) {
          debug(s"check for record: ${url.id.id} was success")
          urlTypeChangeActor ! ChangeUrlType.ChangeTypeToCDN(url)
          urlDataActor ! UrlsDataActor.DeleteUrls(url.id)
        }
        else debug(s"check for record: ${url.id.id} was failed")
      }
  }

  def loadAllUrls(url: Urls): Seq[String] = {
    settings.Job.UrlCheck.imageSizes(getMediaServerSuffixByEntityType(url)).map(MediaHelper.cdnUrl(url.mediaId, _))
  }

}
