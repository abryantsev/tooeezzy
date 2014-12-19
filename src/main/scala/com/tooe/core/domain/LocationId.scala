package com.tooe.core.domain

import org.bson.types.ObjectId

case class LocationId(id: ObjectId = new ObjectId) extends scala.Serializable with ObjectiveId
