package com.tooe.core.domain

import org.bson.types.ObjectId

case class PresentId(id: ObjectId = new ObjectId) extends ObjectiveId

case class PresentStatusId(id: String)

object PresentStatusId {

  val valid = PresentStatusId("valid")
  val received = PresentStatusId("received")
  val expired = PresentStatusId("expired")

}