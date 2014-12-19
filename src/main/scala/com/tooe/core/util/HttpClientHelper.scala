package com.tooe.core.util

import spray.http.{HttpHeaders, HttpRequest}

trait HttpClientHelper {

  implicit def httpMessageHelper(req: HttpRequest) = new {
    def withHostHeader = req withHeaders HttpHeaders.Host(req.uri.authority.host.address)
  }
}