package com.tooe.core.usecase.user

import com.tooe.core.db.mongo.util.{HasIdentityFactory, UnmarshallerEntity, HasIdentity}
import spray.httpx.unmarshalling.{MalformedContent, Deserializer, DeserializationError}


sealed trait UserView extends HasIdentity with UnmarshallerEntity{
  def id: String
}

object UserView extends HasIdentityFactory[UserView] {

  object Short extends UserView {
    def id = "short"
  }
  object AccountProfile extends UserView {
    def id = "account(profile)"
  }
  object AccountPhone extends UserView {
    def id = "account(phone)"
  }
  object AccountPrivate extends UserView {
    def id = "account(private)"
  }
  object AccountMessages extends UserView {
    def id = "account(messages)"
  }

  object AccountImages extends UserView {
    def id = "account(images)"
  }
  object Mini extends UserView {
    def id = "mini"
  }
  object None extends UserView {
    def id = "full"
  }
  val values = Seq(None, Short, AccountProfile, AccountPhone, AccountPrivate, AccountMessages, AccountImages, Mini)
  private val idToVal = values.map(v => v.id -> v).toMap

  def get(id: String) = idToVal.get(id)

  implicit val string2UserView = new Deserializer[String, UserView] {
    def apply(value: String): Either[DeserializationError, UserView] =
      UserView.get(value).map( Right(_)).getOrElse(Left(MalformedContent("'" + value + "' is not a valid UserView value")))
  }
}