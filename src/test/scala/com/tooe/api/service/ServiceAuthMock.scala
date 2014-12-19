package com.tooe.api.service

import com.tooe.core.domain.UserId
import com.tooe.core.usecase.SessionActor.AuthResult
import scala.concurrent.Future

/**
 * Mix to a spray service under test to provide mock authentication
 */
trait ServiceAuthMock { self: AppAuthHelper =>

  val userId = UserId(new org.bson.types.ObjectId)

  override def getAuthResult(authCookies: AuthCookies) =
    Future.successful(AuthResult(userId, authCookies.token, authCookies.timestampOpt))
}