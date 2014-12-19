package com.tooe.core.usecase

import akka.actor.Actor
import com.tooe.core.util.{Lang, ActorHelper}
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.InfoMessageDataService
import concurrent.Future
import akka.pattern.pipe
import com.tooe.core.application.Actors
import com.tooe.core.exceptions.ApplicationException
import spray.http.{StatusCodes, StatusCode}

object InfoMessageActor {
  final val Id = Actors.InfoMessage

  case class GetInfoMessage(id: String)
  case class GetMessage(id: String, lang: String)
  case class GetMessageWithCode(id: String, lang: String)
  case class GetFailure(id: String, lang: Lang, statusCode: StatusCode = StatusCodes.BadRequest)
}

class InfoMessageActor extends Actor with ActorHelper {

  lazy val service = BeanLookup[InfoMessageDataService]

  import context.dispatcher

  import InfoMessageActor._

  def receive = {
    case GetInfoMessage(id) => findInfoMessage(id) pipeTo sender

    case GetMessage(id, lang) => Future {
      def defaultMessage = id
      service.findOne(id) flatMap (_.message.getWithDefaultKeyOpt(id, lang)) getOrElse defaultMessage
    } pipeTo sender

    case GetMessageWithCode(id, lang) =>
      def defaultMessage = InfoMessageWithCode(id, 0)
      val result = service.findOne(id) map (im => InfoMessageWithCode(im.message.getWithDefaultKey(id, lang), im.errorCode)) getOrElse defaultMessage
      sender ! result

    case GetFailure(id, lang, statusCode) =>
      findInfoMessage(id) flatMap { msgOpt =>
        Future failed ApplicationException(
          errorCode = msgOpt map (_.errorCode) getOrElse 0,
          message = msgOpt flatMap (_.message.localized(lang)) getOrElse id,
          statusCode = statusCode
        )
      } pipeTo sender
  }

  def findInfoMessage(id: String) = Future(service.findOne(id))
}


case class InfoMessageWithCode(message: String, errorCode: Int)