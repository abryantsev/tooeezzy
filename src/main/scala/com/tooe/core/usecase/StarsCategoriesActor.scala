package com.tooe.core.usecase

import com.tooe.core.application.{AppActors, Actors}
import akka.actor.Actor
import com.tooe.api.boot.DefaultTimeout
import com.tooe.api.service.SuccessfulResponse
import akka.pattern._
import com.tooe.core.usecase.star_category.StarsCategoriesDataActor
import com.tooe.core.db.mongo.domain.StarCategory
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.domain.{MediaUrl, StarCategoryField}
import com.tooe.core.util.Lang

object StarsCategoriesActor {
  final val Id = Actors.StarsCategories

  case class GetCategories(request: StarCategoriesRequest, lang: Lang)
}

class StarsCategoriesActor extends Actor with AppActors with DefaultTimeout {

  import StarsCategoriesActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val starsCategoriesDataActor = lookup(StarsCategoriesDataActor.Id)

  def receive = {

    case GetCategories(request, lang) =>
      implicit val language = lang
      (starsCategoriesDataActor ? StarsCategoriesDataActor.GetCategories(request)).mapTo[Seq[StarCategory]].map { categories =>
        GetStarCategoriesResponse(categories.map { category =>
          StarCategoriesDetails(Option(category.id).map(_.id),
                                Option(category.name).flatMap(_.localized),
                                Option(category.description).flatMap(_.localized),
                                Option(category.categoryMedia).map(MediaUrl),
                                if(request.fields.map(_.contains(StarCategoryField.StarsCounter)).getOrElse(true)) Option(category.starsCount) else None)
        })
      } pipeTo sender

  }
}

case class GetStarCategoriesResponse(@JsonProperty("starscategories") starsCategories: Seq[StarCategoriesDetails]) extends SuccessfulResponse

case class StarCategoriesDetails(id: Option[String],
                                 name: Option[String],
                                 description: Option[String],
                                 media: Option[MediaUrl],
                                 @JsonProperty("starscounter") count: Option[Long])

case class StarCategoriesRequest(fields: Option[Set[StarCategoryField]])