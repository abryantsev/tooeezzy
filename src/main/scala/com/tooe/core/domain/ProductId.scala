package com.tooe.core.domain

import org.bson.types.ObjectId

case class ProductId(id: ObjectId = new ObjectId) extends ObjectiveId
