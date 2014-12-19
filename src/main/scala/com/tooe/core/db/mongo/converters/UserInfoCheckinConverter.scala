package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.UserInfoCheckin
import com.tooe.core.domain.{MediaObject, Gender, MediaUrl, UserId}

trait UserInfoCheckinConverter extends MediaObjectConverter {

  import DBObjectConverters._

  implicit val userInfoCheckinConverter = new DBObjectConverter[UserInfoCheckin] {

    def serializeObj(obj: UserInfoCheckin) = DBObjectBuilder()
        .field("uid").value(obj.userId)
        .field("n").value(obj.name)
        .field("ln").value(obj.lastName)
        .field("sn").value(obj.secondName)
        .field("m").value(obj.media)
        .field("g").value(obj.gender)

    def deserializeObj(source: DBObjectExtractor) =  UserInfoCheckin(
          userId = source.field("uid").value[UserId],
          name = source.field("n").value[String],
          lastName = source.field("ln").value[String],
          secondName = source.field("sn").opt[String],
          media = source.field("m").opt[MediaObject],
          gender = source.field("g").value[Gender]
      )

  }

}
