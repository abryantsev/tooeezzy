package com.tooe.core.service

import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.domain._
import org.bson.types.ObjectId
import java.util.{UUID, Date}
import com.tooe.api.service.{SentPresentsRequest, OffsetLimit, GetPresentParameters}
import com.tooe.core.db.mongo.domain.{AnonymousRecipient, PresentProductMedia, Present, PresentProduct}
import com.tooe.core.usecase.present.PresentAdminSearchSortType
import scala.util.Random
import org.joda.time.DateTime
import com.tooe.core.db.mongo.query.UpdateResult
import com.tooe.core.util.HashHelper

class PresentDataServiceTest extends SpringDataMongoTestHelper {

  @Autowired var service: PresentDataService = _

  lazy val entities = new MongoDaoHelper("present")

  @Test
  def readWrite() {
    val present = PresentFixture.present()
    service.find(present.id) === None
    service.save(present)
    service.find(present.id) === Some(present)
  }

  @Test
  def representation() {
    val present = PresentFixture.present(hideForUsers = Seq(UserId()))
    service.save(present)
    val repr = entities.findOne(present.id.id)
    jsonAssert(repr)( s"""{
      "_id" : ${present.id.id.mongoRepr} ,
      "c"   : ${present.code.value} ,
      "uid" : ${present.userId.get.id.mongoRepr} ,
      "ar" : {
        "p" : {
          "c": "${present.anonymousRecipient.get.phone.get.code}" ,
          "n": "${present.anonymousRecipient.get.phone.get.number}"
        } ,
        "e" : "${present.anonymousRecipient.get.email.get}"
      } ,
      "sid" : ${present.senderId.id.mongoRepr} ,
      "hs"  : true ,
      "m"   : ${present.message.getOrElse("")} ,
      "t"   : ${present.createdAt.mongoRepr} ,
      "et"  : ${present.expiresAt.mongoRepr} ,
      "rt"  : ${present.receivedAt.get.mongoRepr} ,
      "cs"  : ${present.presentStatusId.get.id} ,
      "p"   : {
        "cid" : ${present.product.companyId.id.mongoRepr} ,
        "lid" : ${present.product.locationId.id.mongoRepr} ,
        "pid" : ${present.product.productId.id.mongoRepr} ,
        "pn"  : ${present.product.productName} ,
        "pm" : {
          "u" : {
            "mu" : ${present.product.media.get.url.url.id} ,
            "t" : ${present.product.media.get.url.mediaType.get.id}
            } ,
          "d" : ${present.product.media.get.description.get}
        } ,
        "pt"  : ${present.product.productTypeId.id} ,
        "d"  : ${present.product.description}
      } ,
      "fid" : "${present.orderId}",
      "ac" : "administrator comment",
      "hid" : [${present.hideForUsers.head.id.mongoRepr}]
    }""")
  }

  @Test
  def find() {
    val present = PresentFixture.present()
    val ids = Set(present.id)
    service.find(ids) === Nil
    service.save(present)
    service.find(ids) === Seq(present)
  }

  @Test
  def getUserPresents {

    val user = UserId()
    val userPresent = PresentFixture.generatePresents(3, user)
    saveEntities(userPresent ++ PresentFixture.generatePresents(3) :+ PresentFixture.present(hideForUsers = Seq(user)))

    val presents = service.getUserPresents(user, GetPresentParameters(None, None), OffsetLimit(Some(0), Some(10)))
    presents must haveSize(3)
    presents must haveTheSameElementsAs(userPresent)

  }

  @Test
  def getUserPresentsProduct {

    val user = UserId()
    val userPresent = PresentFixture.generatePresents(3, user).map(p => p. copy(product = p.product.copy(productTypeId = ProductTypeId.Product)))
    saveEntities(userPresent ++ PresentFixture.generatePresents(3))

    val presents = service.getUserPresents(user, GetPresentParameters(Some("product"), None), OffsetLimit(Some(0), Some(10)))
    presents must haveSize(3)
    presents must haveTheSameElementsAs(userPresent)

  }

  @Test
  def getUserPresentsСertificate {

    val user = UserId()
    val userPresent = PresentFixture.generatePresents(3, user).map(p => p.copy(product = p.product.copy(productTypeId = ProductTypeId.Certificate)))
    saveEntities(userPresent ++ PresentFixture.generatePresents(3))

    val presents = service.getUserPresents(user, GetPresentParameters(Some("certificate"), None), OffsetLimit(Some(0), Some(10)))
    presents must haveSize(3)
    presents must haveTheSameElementsAs(userPresent)

  }

  @Test
  def userPresentsCount {

    val user = UserId()
    val userPresent = PresentFixture.generatePresents(3, user)
    saveEntities(userPresent ++ PresentFixture.generatePresents(3))

    service.userPresentsCount(user, GetPresentParameters(None, None)) === 3

  }

  @Test
  def activatePresent() {
    val present = PresentFixture.present()
    service.save(present)

    val presentFromDB = service.find(present.id)
    presentFromDB.map(_.presentStatusId).get === present.presentStatusId
    presentFromDB.map(_.receivedAt).get === present.receivedAt

    service.activatePresent(present.id)

    val activatedPresent = service.find(present.id)
    activatedPresent.map(_.presentStatusId).get === Some(PresentStatusId("received"))
    activatedPresent.map(_.receivedAt.get).get must beGreaterThan(present.receivedAt.get)
  }

  @Test
  def findByCode() {
    val present = PresentFixture.present(code = UUID.randomUUID().toString)
    service.findByCode(present.code) === None
    service.save(present)
    service.findByCode(present.code) === Some(present)
  }

  @Test
  def commentPresent {
    val present = PresentFixture.present()
    service.save(present)
    val comment = "new administaration comment"
    service.commentPresent(present.id, comment)
    service.find(present.id).flatMap(_.adminComment) === Some(comment)
  }

  @Test
  def findByAdminCriteria() {
    val locationId = LocationId()
    val status = PresentStatusId.valid
    val names = Seq("Cool Name", "cooL NaMe", "COOL NAME")
    val presents = names.map(name => PresentFixture.present(locationId = locationId, productName = name, presentStatusId = status))

    presents.foreach(service.save)

    val params = PresentAdminSearchParams(Some("cool name"), locationId, Some(status), Some(PresentAdminSearchSortType.CreationTime), OffsetLimit(offset = 1, limit = 1))

    val found = service.findByAdminCriteria(params)

    presents.size !== found.size
    found === presents.sortBy(_.createdAt).drop(1).init

  }

  @Test
  def countByAdminCriteria() {
    val locationId = LocationId()
    val status = PresentStatusId.valid
    val names = Seq("Cool Name", "cooL NaMe", "COOL NAME")
    val presents = names.map(name => PresentFixture.present(locationId = locationId, productName = name, presentStatusId = status))

    presents.foreach(service.save)

    val params = PresentAdminSearchParams(Some("cool name"), locationId, Some(status), Some(PresentAdminSearchSortType.CreationTime), OffsetLimit(offset = 1, limit = 1))

    val count = service.countByAdminCriteria(params)

    count === presents.size
  }

  @Test
  def updateMediaStorageToS3 {
    val media = new MediaObjectFixture("url1", UrlType.http).mediaObject
    val present = PresentFixture.present(productsMedia = PresentProductMedia(url = media))
    service.save(present)

    val expectedMedia = new MediaObjectFixture(storage = UrlType.s3).mediaObject

    service.updateMediaStorageToS3(present.id, expectedMedia.url)

    service.find(present.id).flatMap(_.product.media.map(_.url)) === Some(expectedMedia)
  }

  @Test
  def updateMediaStorageToCDN {
    val media = new MediaObjectFixture("url1", UrlType.s3).mediaObject
    val present = PresentFixture.present(productsMedia = PresentProductMedia(url = media))
    service.save(present)

    service.updateMediaStorageToCDN(present.id)

    service.find(present.id).flatMap(_.product.media.map(_.url)) === Some(media.copy(mediaType = None))
  }

  @Test
  def markAsRemoved(){
    val present = PresentFixture.present()
    service.save(present)
    service.markAsRemoved(present.id, present.userId.get) === UpdateResult.Updated
    service.find(present.id).map(_.hideForUsers) === Some(present.userId.toSeq)

    service.markAsRemoved(present.id, present.senderId) === UpdateResult.Updated
    service.find(present.id).map(_.hideForUsers) === Some(present.userId.toSeq :+ present.senderId)
  }

  @Test
  def getUserSentPresents {

    val user = UserId()
    val userPresent = PresentFixture.generatePresents(3, senderId = user)
    saveEntities(userPresent ++ PresentFixture.generatePresents(3) :+ PresentFixture.present(hideForUsers = Seq(user)))

    val presents = service.getUserSentPresents(user, SentPresentsRequest(None, None), OffsetLimit(Some(0), Some(10)))
    presents must haveSize(3)
    presents must haveTheSameElementsAs(userPresent)
    presents === userPresent.reverse

  }

  @Test
  def getUserSentPresentsCount {

    val user = UserId()
    val userPresent = PresentFixture.generatePresents(3, senderId = user)
    saveEntities(userPresent ++ PresentFixture.generatePresents(3))

    service.getUserSentPresentsCount(user, SentPresentsRequest(None, None)) === 3
  }

  def saveEntities(presents: Seq[Present]) = presents.foreach(service.save)

  @Test
  def findUserPresents {
    val productId = ProductId()
    val userId = UserId()
    val p1, p2 = PresentFixture.present(productId = productId, userId = Some(userId))
    val p3 = PresentFixture.present(userId = Some(userId))

    saveEntities(Seq(p1, p2, p3))

    service.findUserPresents(userId, productId) === Seq(p1, p2)
  }

  @Test
  def findByOrderIds {
    val presents = PresentFixture.generatePresents(3)
    presents.foreach(service.save)

    val validPresents = presents.take(2)
    val invalidPresents = presents.drop(2)

    val foundPresents = service.findByOrderIds(validPresents.map(_.orderId.underlying()))

    foundPresents must have size validPresents.length
    foundPresents must haveTheSameElementsAs(validPresents)

    invalidPresents.foreach { present =>
      foundPresents must not contain present
    }
  }

  @Test
  def updatePresentStatus {
    val calendar = java.util.Calendar.getInstance()
    calendar.set(2011, 10, 1)
    val p1 = PresentFixture.present().copy(expiresAt = calendar.getTime, presentStatusId = None)
    val p2 = PresentFixture.present().copy(expiresAt = calendar.getTime, presentStatusId = Some(PresentStatusId.received))
    service.save(p1)
    service.save(p2)
    service.updatePresentStatusForExpiredPresents()

    service.find(p1.id).flatMap(_.presentStatusId) === Some(PresentStatusId.expired)
    service.find(p2.id).flatMap(_.presentStatusId) === Some(PresentStatusId.received)
  }

  @Test
  def assignUserPresentsByEmail {
    val f = new AssignUserPresentFixture
    import f._
    service.save(present)

    service.assignUserPresents(None, anonymousRecipient.email)(userId) === 1
    val found = service.find(present.id).get
    found.userId.get === userId
    found.anonymousRecipient === None
  }

  @Test
  def assignUserPresentsByPhone {
    val f = new AssignUserPresentFixture
    import f._
    service.save(present)

    service.assignUserPresents(anonymousRecipient.phone, None)(userId) === 1
    val found = service.find(present.id).get
    found.userId.get === userId
    found.anonymousRecipient === None
  }

  @Test
  def assignUserPresentsByPhoneOrEmail {
    val f = new AssignUserPresentFixture
    import f._
    service.save(present)

    service.assignUserPresents(anonymousRecipient.phone, Some("wrong.email@ru.ru"))(userId) === 1
    val found = service.find(present.id).get
    found.userId.get === userId
    found.anonymousRecipient === None
  }

  @Test
  def assignUserPresentsWithoutParams {
    val f = new AssignUserPresentFixture
    import f._
    service.save(present)

    service.assignUserPresents(None, None)(userId) === 0
    val found = service.find(present.id).get
    found.userId === None
    found.anonymousRecipient.get === anonymousRecipient
  }
}

object PresentFixture {

  import com.tooe.core.util.SomeWrapper._

  def generatePresents(count: Int, userId: UserId = UserId(), senderId: UserId = UserId()) =
    (1 to count).map {
      value =>
        present(userId = userId, senderId = senderId, createdAt = DateTime.now().plusDays(value).toDate)
    }

  def present(id: PresentId = PresentId(),
              productId: ProductId = ProductId(new ObjectId),
              companyId: CompanyId = CompanyId(new ObjectId),
              userId: Option[UserId] = Some(UserId()),
              senderId: UserId = UserId(),
              productName: String = "some-product-name",
              code: String = "some-code",
              presentStatusId: PresentStatusId = PresentStatusId("some-present-status-id"),
              productTypeId: ProductTypeId = ProductTypeId.Product,
              locationId: LocationId = LocationId(new ObjectId),
              productsMedia: PresentProductMedia = PresentProductMedia(url = MediaObject(MediaObjectId("some-url")), description = "some-description"),
              createdAt: Date = new Date,
              hideForUsers: Seq[UserId] = Nil,
              anonymousRecipient: Option[AnonymousRecipient] = Some(new AnonymousRecipientFixture().anonymousRecipient)) =
    Present(
      id = id,
      code = PresentCode(code),
      userId = userId,
      anonymousRecipient = anonymousRecipient,
      senderId = senderId,
      hideSender = Some(true),
      message = "some-message",
      createdAt = createdAt,
      expiresAt = new Date,
      receivedAt = new Date,
      presentStatusId = presentStatusId,
      product = PresentProduct(
        companyId = companyId,
        locationId = locationId,
        productId = productId,
        productName = productName,
        productTypeId = productTypeId,
        media = productsMedia,
        description = "some-product-description"
      ),
      orderId = Random.nextLong(),
      adminComment = Some("administrator comment"),
      hideForUsers = hideForUsers
    )
}

class AnonymousRecipientFixture {
  val anonymousRecipient = AnonymousRecipient(
    phone = Some(new PhoneShortFixture().phoneShort),
    email = Some(HashHelper.str("email"))
  )
}

class AssignUserPresentFixture {
  val userId = UserId()
  val anonymousRecipient = new AnonymousRecipientFixture().anonymousRecipient
  val present = PresentFixture.present(userId = None, anonymousRecipient = Some(anonymousRecipient))
}