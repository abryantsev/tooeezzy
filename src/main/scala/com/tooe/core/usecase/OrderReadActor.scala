package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.usecase.payment.PaymentDataActor
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.api.service.{ObjectId, SuccessfulResponse}
import java.util.Date
import com.tooe.core.db.mysql.domain.Payment
import java.math.BigInteger
import com.tooe.core.usecase.present.PresentDataActor
import scala.util.Try
import com.tooe.api.JsonProp
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain.{AdminUser, Present}
import com.tooe.core.usecase.admin_user.AdminUserDataActor
import com.tooe.core.domain.AdminUserId
import com.tooe.core.domain.CompanyId
import com.tooe.core.domain.UserId
import com.tooe.core.db.mysql.domain.Recipient
import com.tooe.core.domain.ProductId
import org.joda.time.DateTime

object OrderReadActor {
  final val Id = Actors.OrderRead

  case object GetOrdersFotExport
}

class OrderReadActor extends AppActor {

  import OrderReadActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val paymentDataActor = lookup(PaymentDataActor.Id)
  lazy val presentDateActor = lookup(PresentDataActor.Id)
  lazy val userReadActor = lookup(UserReadActor.Id)
  lazy val adminUserDataActor = lookup(AdminUserDataActor.Id)

  def receive = {

    case GetOrdersFotExport =>
      val exportTime = new Date
      val ordersFetchFuture = (paymentDataActor ? PaymentDataActor.GetPaymentsForExport(exportTime))
                           .zip(paymentDataActor ? PaymentDataActor.GetPaymentsForExportCount(exportTime)).mapTo[(List[Payment], Int)]

      (for {
        (payments, count) <- ordersFetchFuture
        presents <- (presentDateActor ? PresentDataActor.FindByOrderIds(payments.map(_.orderJpaId))).mapTo[Seq[Present]]
        presentsMap = presents.map(present => (present.orderId, present)).toMap
        users <- (userReadActor ? UserReadActor.GetPaymentUsers(payments.map(_.getUserId) ++ payments.flatMap(_.recipient.getRecipientId))).mapTo[Seq[PaymentUser]]
        adminUsers <- (adminUserDataActor ? AdminUserDataActor.FindAdminUsersByCompanyAndRoles(payments.map(_.productSnapshot.getCompanyId), Seq(AdminRoleId.Dealer, AdminRoleId.Superdealer))).mapTo[Seq[AdminUser]]
        adminUsersMap = adminUsers.filter(_.companyId.isDefined).map(user => (user.companyId.get, user)).toMap
      } yield {
        paymentDataActor ! PaymentDataActor.MarkPaymentAsPotentialExported(payments.map(_.orderId), new DateTime(exportTime).plusMinutes(10).toDate)
        ExportOrdersResponse(count, OrderItems(payments, presentsMap, users.toMapId(_.id.id.toString), adminUsersMap))
      }) pipeTo sender

  }

}

case class ExportOrdersResponse(@JsonProperty("ordersexportcount") count: Int, @JsonProperty("ordersexport") items: Seq[OrderItem]) extends SuccessfulResponse

case class OrderItem(
  id: BigInteger,
  @JsonProperty("creationtime") createdTime: Date,
  @JsonProperty("receivingtime") receivingTime: Option[Date],
  @JsonProperty("expirationtime") expirationTime: Date,
  actor: PaymentUser,
  recipient: RecipientPaymentUser,
  product: PaymentProductItem,
  company: PaymentCompanyItem,
  @JsonProperty("currencyid") currencyId: String,
  payment: BigDecimal,
  @JsonProperty("paym_system") paymentSystem: String,
  @JsonProperty("sub_paym_system") subPaymentSystem: Option[String],
  @JsonProperty("paym_transactionid") paymentTransactionId: String)

case class RecipientPaymentUser(@JsonProp("recipientid") id: Option[UserId] = None,
                                name: Option[String] = None,
                                @JsonProp("lastname") lastName: Option[String] = None,
                                email: Option[String] = None,
                                @JsonProp("phone") mainPhone: Option[String] = None,
                                star: Option[Boolean] = None,
                                @JsonProp("staragentid") staragentId: Option[AdminUserId] = None)

object UnregisteredPaymentUser {

  def apply(recipient: Recipient): RecipientPaymentUser =
    RecipientPaymentUser(
      id = recipient.getRecipientId,
      email = Option(recipient.email),
      mainPhone = Option(recipient.phoneCode) -> Option(recipient.phone) match {
        case (None, phone@Some(p)) => phone
        case (code@Some(c), None) => code
        case (code@Some(c), phone@Some(p)) => Some(s"$c $p")
        case _ => None
      }
    )

  def apply(recipient: PaymentUser): RecipientPaymentUser =
    RecipientPaymentUser(
      id = Option(recipient.id),
      name = Option(recipient.name),
      lastName = Option(recipient.lastName),
      email = Option(recipient.email),
      mainPhone = recipient.mainPhone,
      star = recipient.star,
      staragentId = recipient.staragentId
    )

}

case class PaymentProductItem(@JsonProp("productid") id: ProductId, name: String)

case class PaymentCompanyItem(@JsonProp("companyid") id: CompanyId, agentId: Option[AdminUserId])

object OrderItems {

  def apply(payments: Seq[Payment], presentsMap: Map[Payment.OrderId, Present], userMap: Map[String, PaymentUser], adminUserMap: Map[CompanyId, AdminUser]): Seq[OrderItem] =
    payments.map { payment =>
       val present = presentsMap(payment.orderJpaId)
      OrderItem(
        id = payment.orderJpaId,
        createdTime = payment.date,
        receivingTime = present.receivedAt,
        expirationTime = present.expiresAt,
        actor = userMap(payment.userId).copy(star = None, staragentId = None),
        recipient = userMap.get(payment.recipient.recipientId).map(UnregisteredPaymentUser(_)).getOrElse(UnregisteredPaymentUser(payment.recipient)),
        product = PaymentProductItem(payment.productSnapshot.getProductId, payment.productSnapshot.name),
        company = PaymentCompanyItem(payment.productSnapshot.getCompanyId, adminUserMap.get(payment.productSnapshot.getCompanyId).map(_.id)),
        currencyId = payment.currencyId,
        payment = payment.amount,
        paymentSystem = payment.paymentSystem.system,
        subPaymentSystem = Option(payment.paymentSystem.subSystem),
        paymentTransactionId = payment.transactionId
      )
   }

}
