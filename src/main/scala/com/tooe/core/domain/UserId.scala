package com.tooe.core.domain

import org.bson.types.ObjectId

case class UserId(id: ObjectId = new ObjectId) extends scala.Serializable