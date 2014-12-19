package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import org.bson.types.ObjectId
import java.util.Date
import com.tooe.api.service.RegistrationParams
import com.tooe.core.util.HashHelper._
import com.tooe.core.domain.{VerificationKey, CredentialsId, UserId}

@Document(collection = "credentials")
case class Credentials
(
  id: ObjectId = new ObjectId,
  @Field("uid") uid: ObjectId,
  @Field("un") userName: String,
  @Field("pwd") passwordHash: String,
  @Field("lpwd") legacyPasswordHash: Option[String],
  @Field("fid") facebookId: Option[String],
  @Field("vid") vkontakteId: Option[String],
  @Field("tid") twitterId: Option[String],
  @Field("vt") verificationTime: Option[Date],
  @Field("vk") verificationKey: Option[String]
  ) 
{
  def userId = UserId(uid)

  def credentialsId = CredentialsId(id)

  def getVerificationKey = verificationKey map VerificationKey
}

object Credentials {
  def apply(userId: ObjectId, email: String, pwd: String): Credentials = Credentials(
    id = new ObjectId(),
    uid = userId,
    userName = email,
    passwordHash = passwordHash(pwd),
    legacyPasswordHash = None,
    facebookId = None,
    vkontakteId = None,
    twitterId = None,
    verificationTime = None,
    verificationKey = Option(uuid)
  )
  
}