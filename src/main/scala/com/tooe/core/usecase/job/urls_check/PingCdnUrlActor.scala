package com.tooe.core.usecase.job.urls_check

import com.tooe.core.application.{AppActors, Actors}
import com.tooe.core.usecase.AppActor
import scala.concurrent.Future
import akka.io.IO
import spray.can.Http
import spray.httpx.RequestBuilding._
import spray.http.{HttpHeader, StatusCodes, HttpResponse, Uri}
import akka.util.Timeout

object PingCdnUrlActor {
  val Id = Actors.pingCdnUrl

  case class PingUrl(url: String)

}

class PingCdnUrlActor extends AppActor with MediaUrlJobLogger {

  import scala.concurrent.ExecutionContext.Implicits.global
  import PingCdnUrlActor._

  val cacheHeaderName = "X-Cache"
  val validHeaderValues = Seq("Miss from cloudfront", "Hit from cloudfront")

  def checkXCacheHeader(header: Option[HttpHeader]): Boolean = {
    header.exists(header => validHeaderValues.contains(header.value))
  }

  def receive = {
    case PingUrl(url) =>
      implicit val system = context.system
      (for {
        httpResponse <- IO(Http).ask(Head(Uri(url)))(Timeout(5000)).mapTo[HttpResponse]
      } yield {
        val result = httpResponse.status == StatusCodes.OK && checkXCacheHeader(httpResponse.headers.find(_.name == cacheHeaderName))
        debug(s"check for ${url} finished. result: ${if(result) "success" else "failed" }")
        result
      }) pipeTo sender
  }

}
