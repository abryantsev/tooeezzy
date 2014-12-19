package com.tooe.core.migration

import java.util.Date
import com.tooe.core.usecase.{UserEventWriteActor, UpdateStatisticActor}
import com.tooe.core.migration.db.domain.MappingCollection._
import scala.concurrent.Future
import org.bson.types.ObjectId
import com.tooe.core.usecase.present.PresentDataActor
import com.tooe.core.db.mongo.domain._
import com.tooe.core.domain._
import com.tooe.core.usecase.payment.PaymentDataActor
import com.tooe.core.usecase.wish.WishDataActor
import com.tooe.core.usecase.user.UserDataActor
import com.tooe.core.domain.PresentId
import scala.Some
import com.tooe.core.db.mongo.domain.Wish
import com.tooe.core.db.mongo.domain.Present
import com.tooe.core.domain.UserId
import com.tooe.core.db.mongo.domain.PresentProduct
import com.tooe.core.usecase.product.ProductDataActor
import com.tooe.core.migration.api.{DefaultMigrationResult, MigrationResponse}
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.migration.db.domain.{MappingCollection, IdMapping}

object PresentMigratorActor {
  val Id = 'PresentMigrationActor
  case class LegacyPresent(legacyid: Long, code: String, userid: Option[Int],
                           senderid: Int, hidesender: Boolean, message: Option[String],
                           creationtime: Date, expirationtime: Date, receivedtime: Option[Date],
                           status: Option[String], productid: Int, transactionid: Long) extends UnmarshallerEntity
}

class PresentMigratorActor extends MigrationActor {
  import PresentMigratorActor._
  def receive = {
    case lp: LegacyPresent =>
      import lp._
      val future = for {
        userId <- Future.traverse(userid.toSeq)(uid => lookupByLegacyId(uid, user)).map(_.headOption.map(UserId))
        senderId <- lookupByLegacyId(senderid, user)
        product <- findProduct(productid)
        present <- savePresent(lp, userId, senderId, product)
      } yield MigrationResponse(DefaultMigrationResult(legacyid, present.id.id, "present_migrator"))
      future pipeTo sender
  }

  def savePresent(lp: LegacyPresent, userId: Option[UserId], senderId: ObjectId, p: Product): Future[Present] = {
    def savePresent(present: Present): Future[Present] = {
      presentDataActor ! PresentDataActor.Save(present)
      Future successful present
    }

    def addGift(present: Present): Unit =
      userDataActor ! UserDataActor.AddGift(present.senderId, present.id)

    def incrementPresentsCount(present: Present): Unit = {
      present.userId.foreach {
        userId =>
          updateStatisticActor ! UpdateStatisticActor.ChangeUserNewPresentsCounter(userId, 1)
          present.product.productTypeId match {
            case ProductTypeId.Product =>
              updateStatisticActor ! UpdateStatisticActor.ChangeUserPresentsCounter(userId, 1)
            case ProductTypeId.Certificate =>
              updateStatisticActor ! UpdateStatisticActor.ChangeUserCertificatesCounter(userId, 1)
            case _ =>
          }
      }
      present.product.productTypeId match {
        case ProductTypeId.Product =>
          updateStatisticActor ! UpdateStatisticActor.ChangeUserSentPresentsCounter(present.senderId, 1)
        case ProductTypeId.Certificate =>
          updateStatisticActor ! UpdateStatisticActor.ChangeUserSentCertificatesCounter(present.senderId, 1)
        case _ =>
      }
    }

    def incrementSalesCount(present: Present): Unit = {
      updateStatisticActor ! UpdateStatisticActor.ChangeSalesCounter(present.product.locationId, 1)
      updateStatisticActor ! UpdateStatisticActor.ChangeLocationPresentCounter(present.product.locationId, 1)
    }

    val present = Present(
      id = PresentId(),
      code = PresentCode(lp.code),
      userId = userId,
      anonymousRecipient = None,
      senderId = UserId(senderId),
      hideSender = Some(lp.hidesender),
      message = lp.message,
      createdAt = lp.creationtime,
      expiresAt = lp.expirationtime,
      product = PresentProduct(
        companyId = p.companyId,
        locationId = p.location.id,
        productId = p.id,
        productName = p.name.localized.get,
        productTypeId = p.productTypeId,
        description = p.description.localized.get,
        media = p.productMedia.headOption.map(media => PresentProductMedia(
          url = media.media,
          description = None
        ))),
      orderId = BigInt(lp.transactionid),
      adminComment = None,
      lifeCycle = Some(PresentLifecycleId.Archived)
    )

    val future = savePresent(present)

    future onSuccess PartialFunction(addGift)
    future onSuccess PartialFunction(incrementSalesCount)
    future onSuccess PartialFunction(incrementPresentsCount)
    future onSuccess { case result =>
      idMappingDataActor ? IdMappingDataActor.SaveIdMapping(
        IdMapping(new ObjectId(), MappingCollection.present, lp.legacyid.toInt, present.id.id))
    }
    future
  }

  def findProduct(legacyProductId: Int): Future[Product] = {
    lookupByLegacyId(legacyProductId, product).flatMap {
      productId =>
        (productDataActor ? ProductDataActor.GetProduct(ProductId(productId))).mapTo[Product]
    }
  }

  lazy val productDataActor = lookup(ProductDataActor.Id)
  lazy val userEventWriteActor = lookup(UserEventWriteActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val presentDataActor = lookup(PresentDataActor.Id)
  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val wishDataActor = lookup(WishDataActor.Id)
  lazy val paymentDataActor = lookup(PaymentDataActor.Id)
}