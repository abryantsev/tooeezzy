package com.tooe.core.usecase

import com.tooe.core.usecase.currency.CurrencyDataActor
import com.tooe.core.util.Lang
import com.tooe.core.application.Actors
import com.tooe.api.service.SuccessfulResponse
import akka.pattern.{ask, pipe}
import com.tooe.core.db.mongo.domain.Currency

object CurrencyActor {

  final val Id = Actors.Currency

  case class GetAllCurrencies(lang: Lang)

}

class CurrencyActor extends AppActor {

  import CurrencyActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val currencyDataActor = lookup(CurrencyDataActor.Id)

  def receive = {
    case GetAllCurrencies(lang) =>
      implicit val l = lang
      val result = for {
        c <- getAllCurrencies
        sorted = c.sortWith(_.name.localized.getOrElse("") < _.name.localized.getOrElse(""))
      } yield {
        GetAllCurrenciesResponse(sorted.map(GetAllCurrenciesResponseItem(lang)))
      }
      result.pipeTo(sender)
  }

  def getAllCurrencies =
    currencyDataActor.ask(CurrencyDataActor.FindAll).mapTo[Seq[Currency]]


}

case class GetAllCurrenciesResponse(currencies: Seq[GetAllCurrenciesResponseItem]) extends SuccessfulResponse

case class GetAllCurrenciesResponseItem(id: String, name: String, currentcurs: BigDecimal, centralbankcode: Int)

object GetAllCurrenciesResponseItem {

  def apply(lang: Lang)(c: Currency): GetAllCurrenciesResponseItem = GetAllCurrenciesResponseItem(c.id.id, c.name.localized(lang).getOrElse(""), c.curs, c.numcode)

}
