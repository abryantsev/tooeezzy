package com.tooe.core.util

import com.tooe.core.usecase.{InfoMessageWithCode, InfoMessageActor}
import com.tooe.core.application.AppActors
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.exceptions.ApplicationException
import akka.pattern.ask
import com.tooe.core.main.SharedActorSystem

object InfoMessageHelper extends AppActors with DefaultTimeout{
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit lazy val system =  SharedActorSystem.sharedMainActorSystem

  lazy val infoMessageActor = lookup(InfoMessageActor.Id)

  def throwAppExceptionById(id: String)(implicit lang: Lang) =
    infoMessageActor.ask(
      InfoMessageActor.GetMessageWithCode(id, lang.id)
    ).mapTo[InfoMessageWithCode].map(im => throw ApplicationException(im.errorCode, im.message))
}
