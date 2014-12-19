package com.tooe.api.service

import akka.actor.ActorSystem
import com.tooe.core.usecase._
import akka.pattern.ask
import com.tooe.core.payment.plantron.PaymentSystemResponse
import spray.routing.PathMatcher
import scala.util.Try
import com.tooe.api.validation.{ValidationFailed, ValidationException}
import spray.http.StatusCodes

class PaymentSystemService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  import PaymentSystemService._
  import PaymentSystemActor._

  lazy val paymentSystemActor = lookup(PaymentSystemActor.Id)

  implicit val ec = system.dispatcher

  val route =
    (mainPrefix & path(Path.Root)) { implicit routeContext: RouteContext =>
      get {
        parameters('amount, 'currency) { (amountStr, currency) =>
          authenticateBySession {  _: UserSession =>
            //TODO validate amountStr which should be BigDecimal
            val amount = payment.Amount(
              value = Try(BigDecimal(amountStr)).getOrElse(throw ValidationException(ValidationFailed("Incorrect amount value"), StatusCodes.BadRequest)),
              currency = currency)
            complete((paymentSystemActor ? GetPaymentSystems(amount))(timeout).mapTo[PaymentSystemResponse])
          }
        }
      }
    }
}

object PaymentSystemService {
  object Path {
    val Root = PathMatcher("paymentsystems")
  }
}