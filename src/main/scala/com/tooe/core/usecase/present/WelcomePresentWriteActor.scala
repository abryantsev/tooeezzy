package com.tooe.core.usecase.present

import com.tooe.core.usecase.AppActor
import com.tooe.core.application.Actors
import com.tooe.core.domain.UserId
import com.tooe.core.util.{Lang, DateHelper}

object WelcomePresentWriteActor {

  final val Id = Actors.WelcomePresentWrite

  case class MakeWelcomePresent(userId: UserId)
}

class WelcomePresentWriteActor extends AppActor with WelcomePresentConfigHelper {

  lazy val freePresentWriteActor = lookup(FreePresentWriteActor.Id)

  import WelcomePresentWriteActor._

  def receive = {
    case MakeWelcomePresent(userId) =>
      val curTime = DateHelper.currentDate
      if (curTime.compareTo(ValidFrom) < 0 || curTime.compareTo(ValidTill) > 0)
        log.info(s"Welcome present will not be given due to validation constraints [$ValidFrom, $ValidTill]")
      else {
        val req = FreePresentWriteActor.MakeFreePresent(
          productId = Product,
          actorId = Presenter,
          recipientId = userId,
          message = Message,
          isPrivate = false,
          hideActor = true,
          lang = Lang.ru
          )
        log.info(s"Welcome present is going to be given [$ValidFrom, $ValidTill]: $req")
        freePresentWriteActor ! req
      }
  }
}
