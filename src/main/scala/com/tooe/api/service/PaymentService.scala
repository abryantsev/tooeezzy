package com.tooe.api.service

import spray.http._
import spray.routing.{PathMatcher, Directives}
import com.tooe.core.usecase._
import com.tooe.core.usecase.payment._
import akka.actor.ActorSystem
import com.tooe.core.usecase.payment.alfabank.AlfabankStrategyActor.ReturnUrlCallbackResponse
import com.tooe.core.usecase.payment.alfabank.AlfabankStrategyActor
import spray.routing.RequestContext
import com.tooe.core.usecase.payment.alfabank.AlfabankStrategyActor.AlfabankInitPaymentResponse
import com.tooe.core.usecase.payment.PlatronStrategyActor.PlatronInitPaymentResponse
import com.tooe.core.usecase.PaymentCompleteJson
import com.tooe.core.usecase.payment.tooeezzy.TooeezzyStrategyActor
import com.tooe.core.payment.plantron.{XmlMessageHelper, PlatronParams, PlatronFacade}

class PaymentService(implicit system: ActorSystem) extends SprayServiceBaseClass2 {

  import PaymentService._
  import akka.pattern.ask

  implicit val ec = system.dispatcher

  lazy val platronStrategyActor = lookup(PlatronStrategyActor.Id)
  lazy val alfabankStrategyActor = lookup(AlfabankStrategyActor.Id)

  lazy val paymentResultUrlReadActor = lookup(PaymentResultUrlReadActor.Id)
  lazy val tooeezzyStrategyActor = lookup(TooeezzyStrategyActor.Id)

  val route =
    (mainPrefix & path(Path.RootPath)) { routeContext: RouteContext =>
      post {
        authenticateBySession { userSession: UserSession =>
          entity(as[payment.PaymentRequest]) { paymentRequest => implicit ctx =>
            val initPaymentRequest = InitPaymentRequest(paymentRequest, routeContext, userSession.userId)
            paymentRequest.paymentSystem.system match {
              case PaymentSystem.Tooeezzy =>
                val future = (tooeezzyStrategyActor ? initPaymentRequest).mapTo[TooeezzyStrategyActor.RedirectTo]
                future onSuccess { case TooeezzyStrategyActor.RedirectTo(url) => ctx.redirect(url, StatusCodes.Found) }
                future onFailure { case t => ctx.failWith(t) }
              case "EGATEWAY" =>
                val future = (alfabankStrategyActor ? initPaymentRequest).mapTo[AlfabankInitPaymentResponse]
                future onSuccess { case AlfabankInitPaymentResponse(url) => ctx.redirect(url, StatusCodes.Found) }
                future onFailure { case t => ctx.failWith(t) }
              case _          =>
                val future = platronStrategyActor.ask(initPaymentRequest).mapTo[PlatronInitPaymentResponse]
                future onSuccess { case PlatronInitPaymentResponse(url) => ctx.redirect(url, StatusCodes.Found) }
                future onFailure { case t => ctx.failWith(t) }
            }
          }
        }
      }
    } ~
    (mainPrefix & path(Path.RootOrder / Segment / Path.SuccessSuffixPath)) { (routeContext: RouteContext, paymentUuid) =>
      get {
        paymentCompleteResult(PaymentCompleteStatus.Ok)(routeContext)
      }
    } ~
    (mainPrefix & path(Path.RootOrder / Segment / Path.FailureSuffixPath)) { (routeContext: RouteContext, paymentUuid) =>
      get {
        paymentCompleteResult(PaymentCompleteStatus.Error)(routeContext)
      }
    } ~
    (mainPrefix & path(Path.RootOrder / Segment / Path.StatusSuffixPath)) { (routeContext: RouteContext, paymentUuid) =>
      get {
        parameter('orderId, 'responsetype.as[ResponseType]) { (orderUuid, responseType) => implicit ctx =>
          val msg = AlfabankStrategyActor.ReturnUrlCallback(paymentUuid, orderUuid, routeContext.lang, responseType)
          val future = (alfabankStrategyActor ? msg).mapTo[ReturnUrlCallbackResponse]
          future onSuccess { case ReturnUrlCallbackResponse(url) => ctx.redirect(url, StatusCodes.Found) }
          future onFailure { case t => ctx.failWith(t) }
        }
      }
    } ~
    (mainPrefix & path(Path.CallbackPath)) { routeContext: RouteContext =>
      parameters('system ?, 'action ?).as(CallbackParams) { callbackParams =>
        post {
          platronCallbackHandler(script = Path.Callback)
        }
      }
    }

  private def platronCallbackHandler(script: String) =
    entity(as[String]) { body => implicit ctx =>
      val platronParams = PlatronFacade.parseXml(body) //TODO may cause SAXParseException
      val future = platronStrategyActor.ask(PlatronStrategyActor.PlatronCallback(script, platronParams))
        .mapTo[PlatronParams] map { signedResponse =>
          val response = XmlMessageHelper.build('response, signedResponse)
          HttpEntity(ContentType(MediaTypes.`text/plain`, HttpCharsets.`UTF-8`), response.getBytes("UTF-8"))
        }
      future onSuccess { case r => ctx.complete(r) }
      future onFailure onPaymentCallbackFailure(ctx)
    }

  private def onPaymentCallbackFailure(ctx: RequestContext): PartialFunction[Throwable, Any] = {
    case IncorrectSignature(_) => //do nothing
    case t => ctx.failWith(t)
  }

  private def paymentCompleteResult(status: PaymentCompleteStatus)(implicit routeContext: RouteContext) =
    parameter('responsetype.as[ResponseType]) { responseType => (ctx: RequestContext) => {
      (paymentResultUrlReadActor ? PaymentResultUrlReadActor.GetPaymentResultUrl(status, responseType, routeContext.lang)) onSuccess {
        case PaymentCompleteHtml(url)  => ctx.redirect(url, StatusCodes.Found)
        case resp: PaymentCompleteJson => ctx.complete(resp)
      }
    }}
}

object PaymentService extends Directives {
  object Path {
    val Root = "payment"
    val RootPath = PathMatcher(Root)

    val Callback = "paymentaction"
    val CallbackPath = PathMatcher(Callback)

    private val Order = "order"
    private val OrderPath = PathMatcher(Order)

    val RootOrder = RootPath / OrderPath

    val StatusSuffix = "status"
    val StatusSuffixPath = PathMatcher(StatusSuffix)
    def status(orderId: String) = s"$Root/$Order/$orderId/$StatusSuffix"

    val SuccessSuffix = "success"
    val SuccessSuffixPath = PathMatcher(SuccessSuffix)
    def success(orderId: String) = s"$Root/$Order/$orderId/$SuccessSuffix"

    val FailureSuffix = "failure"
    val FailureSuffixPath = PathMatcher(FailureSuffix)
    def failure(orderId: String) = s"$Root/$Order/$orderId/$FailureSuffix"
  }
}