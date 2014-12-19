package com.tooe.api.service

import com.tooe.core.domain.UserId
import com.tooe.core.domain.SessionToken
import spray.http.HttpHeaders.Cookie
import spray.http.HttpCookie

object SessionCookieFixture extends SessionCookieFixture

trait SessionCookieFixture {

  def userId = UserId(ObjectId())
  def sessionToken = SessionToken("some-session-token")
  def timestamp = "some-time-stamp"
  
  def SessionTokenCookie = Cookie(HttpCookie(SessionCookies.Token, sessionToken.hash))
  def SessionTimestampCookie = Cookie(HttpCookie(SessionCookies.Timestamp, timestamp))
  
  def sessionCookies = SessionTokenCookie :: SessionTimestampCookie :: Nil
}