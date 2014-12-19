package com.tooe.core.domain

import org.bson.types.ObjectId

case class CompanyId(id: ObjectId = new ObjectId) extends ObjectiveId
