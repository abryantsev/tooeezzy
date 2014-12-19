package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{UserPhoneId, UserId}

@Document(collection = "user_phones")
case class UserPhone
(
  id: UserPhoneId = UserPhoneId(),
  userId: UserId,
  phone: Phone
  )




