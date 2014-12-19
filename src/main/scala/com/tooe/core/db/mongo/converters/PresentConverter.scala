package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import com.tooe.core.domain._
import java.util.Date
import com.tooe.core.db.mongo.domain.{AnonymousRecipient, Present, PresentProduct}

@WritingConverter
class PresentWriteConverter extends Converter[Present, DBObject] with PresentConverter {
  def convert(source: Present) = presentConverter.serialize(source)
}

@ReadingConverter
class PresentReadConverter extends Converter[DBObject, Present] with PresentConverter {
  def convert(source: DBObject) = presentConverter.deserialize(source)
}

trait PresentConverter extends PresentProductConverter {

  import DBObjectConverters._
  import DBCommonConverters._

  implicit val anonymousRecipientConverter = new DBObjectConverter[AnonymousRecipient] {
    def serializeObj(obj: AnonymousRecipient) = DBObjectBuilder()
      .field("p").value(obj.phone)
      .field("e").value(obj.email)

    def deserializeObj(source: DBObjectExtractor) = AnonymousRecipient(
      phone = source.field("p").opt[PhoneShort],
      email = source.field("e").opt[String]
    )
  }

  implicit val presentConverter = new DBObjectConverter[Present] {
    def serializeObj(obj: Present) = DBObjectBuilder()
      .id.value(obj.id)
      .field("c").value(obj.code)
      .field("uid").value(obj.userId)
      .field("ar").value(obj.anonymousRecipient)
      .field("sid").value(obj.senderId)
      .field("hs").value(obj.hideSender)
      .field("m").value(obj.message)
      .field("t").value(obj.createdAt)
      .field("et").value(obj.expiresAt)
      .field("rt").value(obj.receivedAt)
      .field("cs").value(obj.presentStatusId)
      .field("p").value(obj.product)
      .field("fid").value(obj.orderId)
      .field("ac").value(obj.adminComment)
      .field("lfs").value(obj.lifeCycle)
      .field("hid").value(obj.hideForUsers)

    def deserializeObj(source: DBObjectExtractor) = Present(
      id = source.id.value[PresentId],
      code = source.field("c").value[PresentCode],
      userId = source.field("uid").opt[UserId],
      anonymousRecipient = source.field("ar").opt[AnonymousRecipient],
      senderId = source.field("sid").value[UserId],
      hideSender = source.field("hs").opt[Boolean],
      message = source.field("m").opt[String],
      createdAt = source.field("t").value[Date],
      expiresAt = source.field("et").value[Date],
      receivedAt = source.field("rt").opt[Date],
      presentStatusId = source.field("cs").opt[PresentStatusId],
      product = source.field("p").value[PresentProduct],
      orderId = source.field("fid").value[BigInt],
      adminComment = source.field("ac").opt[String],
      lifeCycle = source.field("lfs").opt[PresentLifecycleId],
      hideForUsers = source.field("hid").seq[UserId]
    )
  }
}