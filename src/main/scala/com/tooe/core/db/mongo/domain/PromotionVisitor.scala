package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date
import com.tooe.core.domain.{PromotionStatus, PromotionVisitorId, UserId, PromotionId}

@Document(collection = "promotion_visitor")
case class PromotionVisitor
(
  id: PromotionVisitorId = PromotionVisitorId(),
  promotion: PromotionId,
  visitor: UserId,
  status: PromotionStatus,
  time: Date
  )