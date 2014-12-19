package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.UserStar
import com.tooe.core.domain.{AdminUserId, StarCategoryId}

trait UserStarConverter {
  import DBObjectConverters._

  implicit val UserStarConverter = new DBObjectConverter[UserStar] {
    def serializeObj(obj: UserStar) = DBObjectBuilder()
      .field("sc").value(obj.starCategoryIds)
      .field("pm").value(obj.presentMessage)
      .field("suc").value(obj.subscribersCount)
      .field("aid").value(obj.agentId)

    def deserializeObj(source: DBObjectExtractor) = UserStar(
      starCategoryIds = source.field("sc").seq[StarCategoryId],
      presentMessage = source.field("pm").opt[String],
      subscribersCount = source.field("suc").value[Int](0),
      agentId = source.field("aid").opt[AdminUserId]
    )
  }
}