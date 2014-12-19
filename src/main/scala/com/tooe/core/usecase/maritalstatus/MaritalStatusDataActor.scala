package com.tooe.core.usecase.maritalstatus

import com.tooe.core.usecase.AppActor
import com.tooe.core.application.Actors
import com.tooe.core.domain.MaritalStatusId
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.MaritalStatusDataService
import scala.concurrent.Future
import akka.pattern.pipe

object MaritalStatusDataActor {
  final val Id = Actors.MaritalStatusData

  case class Find(ids: Set[MaritalStatusId])
  case class GetMaritalStatus(id: MaritalStatusId)
  case object FindAll

}

class MaritalStatusDataActor extends AppActor {

  lazy val service = BeanLookup[MaritalStatusDataService]

  import MaritalStatusDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  def receive = {
    case Find(ids) => Future(service.find(ids)) pipeTo sender
    case GetMaritalStatus(id) => Future(service.findOne(id)) pipeTo sender
    case FindAll => Future(service.all) pipeTo sender
  }
}