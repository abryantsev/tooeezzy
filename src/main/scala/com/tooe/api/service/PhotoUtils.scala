package com.tooe.api.service

import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.api.validation.{ValidationContext, Validatable}
import com.fasterxml.jackson.annotation.JsonProperty

//TODO very similar to com.tooe.api.service.AddPhotoRequest either one should be removed
@deprecated("Separate logic for two different classes: url and mimetype + encoding + value")
case class Photo(mimetype: Option[String], encoding: Option[String], value: Option[String], name: Option[String], url: Option[String])
  extends UnmarshallerEntity with Validatable {

  def validate(ctx: ValidationContext) {

    if(url.isDefined && (mimetype.isDefined || encoding.isDefined || value.isDefined))
      ctx.fail("Must be specified one of the url or the photo details")
    if(url.isEmpty && (mimetype.isEmpty || encoding.isEmpty || value.isEmpty))
      ctx.fail("Must be specified photo details: name, encoding, value and mimetype")
  }

  def toPhotoFormat: Option[PhotoFormat] =
    for (m <- mimetype; e <- encoding) yield PhotoFormat(mimetype = m, encoding = e)
}

case class AddPhotoParams(filePath: String, photoFormat: Option[PhotoFormat], name: Option[String])

case class PhotoFormat(mimetype: String, encoding: String)

case class PhotoMessage(@JsonProperty("msg") message: String) extends UnmarshallerEntity