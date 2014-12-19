package com.tooe.core.db.mongo.listeners

import com.tooe.core.db.mongo.domain.User
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener
import com.mongodb.DBObject


class GenderForUserEventListener extends AbstractMongoEventListener[User] {

  override def onAfterLoad(dbo: DBObject) {
    /*dbo.get("g") match {
      case g if g == "m" => dbo.put("g","MALE")
      case g if g == "f" => dbo.put("g","FEMALE")
    }*/
    super.onAfterLoad(dbo)
  }

  override def onBeforeSave(source: User, dbo: DBObject) {
    /*dbo.get("g") match {
      case g if g == "MALE" => dbo.put("g","m")
      case g if g == "FEMALE" => dbo.put("g","f")
    }*/
    super.onBeforeSave(source, dbo)
  }
}