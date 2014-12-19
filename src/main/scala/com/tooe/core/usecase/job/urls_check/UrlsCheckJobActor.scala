package com.tooe.core.usecase.job.urls_check

import com.tooe.core.application.Actors
import com.tooe.core.usecase.AppActor
import com.tooe.core.usecase.urls.UrlsDataActor
import com.tooe.core.db.mongo.domain.Urls
import com.tooe.core.domain.UrlType
import org.joda.time.DateTime

object UrlsCheckJobActor {
  val Id = Actors.urlsCheckJob

  case class CheckUrls(urlType: UrlType)

}

class UrlsCheckJobActor extends AppActor with MediaUrlJobLogger {

  import scala.concurrent.ExecutionContext.Implicits.global
  import UrlsCheckJobActor._

  lazy val urlDataActor = lookup(UrlsDataActor.Id)
  lazy val cdnUrlChecker = lookup(CdnUrlCheckerActor.Id)
  lazy val uploadHttpImageActor = lookup(UploadHttpImageActor.Id)

  def receive = {

    case CheckUrls(urlType) =>
      (urlDataActor ? UrlsDataActor.GetLastUrls(settings.Job.UrlCheck.size, urlType)).mapTo[Seq[Urls]].map { urls =>
        debug(s"load ${urls.size} urls. start checking.")
        urlDataActor ! UrlsDataActor.SetUrlsReadTime(urls.map(_.id), DateTime.now().plusMillis(settings.Job.UrlCheck.readOffset.toMillis.toInt).toDate)
        urls.foreach {
          case url if url.urlType == Some(UrlType.s3) => cdnUrlChecker ! CdnUrlCheckerActor.UrlCheck(url)
          case url if url.urlType == Some(UrlType.http) => uploadHttpImageActor ! UploadHttpImageActor.UploadImage(url)
        }
      }

  }

}
