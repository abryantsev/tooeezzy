package com.tooe.core.usecase.session

import akka.actor.Actor
import com.tooe.core.util.ActorHelper
import com.tooe.core.domain.UserId
import com.tooe.core.db.mongo.domain.User
import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.CacheSessionDataService
import scala.concurrent.Future
import akka.pattern.pipe
import com.tooe.core.exceptions.ApplicationException
import spray.http.StatusCodes
import com.tooe.core.application.Actors
import com.tooe.core.util.HashHelper
import com.tooe.core.db.mongo.domain.CacheSession
import com.tooe.core.domain.SessionToken
import com.tooe.core.util.DateHelper

object CacheSessionDataActor {
  final val Id = Actors.CacheSessionData
  
  case class NewSession(userId: UserId, login: String)
  case class GetSession(token: SessionToken)
  case class Delete(token: SessionToken)
}

class CacheSessionDataActor extends Actor with ActorHelper {

  lazy val service = BeanLookup[CacheSessionDataService]

  import CacheSessionDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global
  
  def receive = {
    case NewSession(userId, login) => Future {
      val cacheSession = createSession(userId, login)
      service.save(cacheSession) 
    } pipeTo sender
    
    case GetSession(token) => Future {
      service.find(token) getOrElse (throw sessionNotFoundException)
    } pipeTo sender
    
    case Delete(token) => Future {
      service.delete(token)
    }
  }
  
  def sessionNotFoundException =
    ApplicationException(message = "Either user has not authenticated or session has expired", statusCode = StatusCodes.Unauthorized)

  def createSession(userId: UserId, login: String) =
    CacheSession(
      id = token(login), 
      createdAt = DateHelper.currentDate, 
      userId = userId
    )
  
  def token(login: String) = {
    val hash = HashHelper.sha1(login + ":" + HashHelper.uuid)
    SessionToken(hash)
  }
}