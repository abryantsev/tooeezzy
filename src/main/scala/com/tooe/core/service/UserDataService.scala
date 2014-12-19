package com.tooe.core.service

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.db.mongo.repository.UserRepository
import com.tooe.core.domain._
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import java.util.regex.Pattern
import com.tooe.api.service.{OffsetLimit, SearchStarsRequest}
import com.tooe.core.util.BuilderHelper
import com.mongodb.BasicDBObject
import com.tooe.core.usecase.user._
import com.tooe.core.db.mongo.converters._
import scala.Predef._
import scala.collection.JavaConverters._
import com.tooe.core.usecase._
import com.tooe.core.db.mongo.domain._
import com.tooe.core.usecase.SearchAmongOwnFriendsRequest
import com.tooe.core.usecase.SearchUsersRequest
import com.tooe.core.db.mongo.converters.DBObjectBuilder
import com.tooe.core.usecase.user.UpdateFields
import com.tooe.core.usecase.user.UpdateUserStatistic
import com.tooe.core.db.mongo.query._

trait UserDataService {
  val topStarsCount = 20

  def save(entity: User): User

  def findOne(id: UserId): Option[User]

  def findUserPhones(id: UserId): Option[UserPhones]

  def findUserIdByEmail(email: String): Option[UserId]

  def findUserIdByPhone(code: String, number: String): Option[UserId]

  def findUsersByUserIds(userIds: Seq[UserId]): Seq[User]

  def findAndFilterUsersBy(userIds: Seq[UserId], request: SearchAmongOwnFriendsRequest, offsetLimit: OffsetLimit): Seq[User]

  def countFindAndFilterUsers(userIds: Seq[UserId], request: SearchAmongOwnFriendsRequest): Long

  def searchUsers(request: SearchUsersRequest): Seq[User]

  def searchUsersCount(request: SearchUsersRequest): Int

  def searchStars(request: SearchStarsRequest, offsetLimit: OffsetLimit): Seq[User]

  def searchStarsCount(request: SearchStarsRequest): Long

  def addGift(userId: UserId, presentId: PresentId): UpdateResult

  def findAbsentIds(ids: Seq[UserId]): Seq[UserId]

  def addPhotoToUser(userId: UserId, photoId: PhotoId): Unit

  def updateUserLastPhotos(userId: UserId, photoIds: Seq[PhotoId]): Unit

  def updateUser(id: UserId, fields: UpdateFields): Unit

  def getUserMedia(userId: UserId): User

  def delete(id: UserId): Unit

  def setUserStatistic(userId: UserId, updater: UpdateUserStatistic): Unit

  def changeUserStatistic(userId: UserId, updater: UpdateUserStatistic): Unit

  def changeUsersStatistics(userIds: Set[UserId], updater: UpdateUserStatistic): Unit

  def getUserStatistics(userId: UserId): UserStatistics

  def addPhotoAlbum(userId: UserId, id: PhotoAlbumId): Unit

  def removePhotoAlbum(userId: UserId, id: PhotoAlbumId): Unit

  def addWish(userId: UserId, wishId: WishId): Unit

  def removeWish(userId: UserId, wishId: WishId): Unit

  def getUserOnline(userId: UserId): UserOnlineStatus

  def searchUserFriends(usersIds: Seq[UserId], request: SearchUserFriendsRequest, offsetLimit: OffsetLimit): Seq[User]

  def countSearchUserFriends(usersIds: Seq[UserId], request: SearchUserFriendsRequest): Long

  def updateUserMedia(userId: UserId, userMedia: Seq[UserMedia], optimisticLock: Option[Int]): UpdateResult

  def updateMediaStorageToS3(userId: UserId, media: MediaObjectId, newMedia: MediaObjectId): Unit

  def updateMediaStorageToCDN(userId: UserId, media: MediaObjectId): Unit

  def findTopStars: Seq[User]
}

@Service
class UserDataServiceImpl extends UserDataService with UserMediaConverter {
  @Autowired var repo: UserRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[User]

  private val starCriteria = new Criteria("star").exists(true)

  val starParamsToMongoFieldsNames: Map[StarField, String] = {
    import StarField._
    Map(
      Id -> "id",
      Name -> "n",
      Lastname -> "ln",
      Address -> "c",
      Media -> "um",
      Subscribers -> "star.suc"
    )
  }

  def getUserOnline(userId: UserId) = repo.getUserOnlineStatus(userId.id)

  def setUserStatistic(userId: UserId, updater: UpdateUserStatistic) {
    val query = Query.query(new Criteria("_id").is(userId.id))
    val update = UpdateUserStatisticHelper.setStatistic(new Update(), updater)
    mongo.updateFirst(query, update, entityClass)
  }

  def changeUserStatistic(userId: UserId, updater: UpdateUserStatistic) {
    val query = Query.query(new Criteria("_id").in(userId.id))
    val update = UpdateUserStatisticHelper.changeStatistic(new Update(), updater)
    if(update.nonEmpty)
      mongo.updateFirst(query, update, entityClass)
  }
  def changeUsersStatistics(userIds: Set[UserId], updater: UpdateUserStatistic) {
    val query = Query.query(new Criteria("_id").in(userIds.map(_.id).asJavaCollection))
    val update = UpdateUserStatisticHelper.changeStatistic(new Update(), updater)
    if(update.nonEmpty)
      mongo.updateMulti(query, update, entityClass)
  }

  def save(entity: User) = repo.save(entity)

  def findOne(id: UserId) = Option(repo.findOne(id.id))

  def findUserPhones(id: UserId) = Option(repo.findUserPhones(id.id)) map (_.contact.phones)

  def delete(id: UserId) { repo.delete(id.id)}

  def findUserIdByEmail(email: String) = Option(repo.findUserByEmail(email)) map (_.id)

  def findUserIdByPhone(code: String, number: String): Option[UserId] =
    repo.findUserIdByPhone(code, number).asScala.headOption map (_.id)

  def findUsersByUserIds(userIds: Seq[UserId]) = repo.findUsersByUserIds(userIds map (_.id)).asScala.toSeq

  def findAndFilterUsersBy(userIds: Seq[UserId], request: SearchAmongOwnFriendsRequest, offsetLimit: OffsetLimit): Seq[User] =  {
    val query = findAndFilterQuery(userIds, request).withPaging(offsetLimit)
    query.fields.exclude("ns")
    mongo.find(query, entityClass).asScala.toSeq
  }

  private[this] def findAndFilterQuery(userIds: Seq[UserId], request: SearchAmongOwnFriendsRequest): Query = {
    import BuilderHelper._
    Query.query(
      new Criteria("_id").in(userIds.map(_.id).asJavaCollection)
        .extend(request.name)(name => _.and("ns").in(Pattern.compile(s"^${name.toLowerCase}")))
        .extend(request.country)(country => _.and("c.a.co").regex(Pattern.compile(country)))

    )
  }

  def countFindAndFilterUsers(userIds: Seq[UserId], request: SearchAmongOwnFriendsRequest) =
    mongo.count(findAndFilterQuery(userIds, request), entityClass)

  private def searchUsersQuery(request: SearchUsersRequest) = {
    import BuilderHelper._
    import request._
      Query.query(new Criteria()
        .extend(name)(name => _.and("ns").in(Pattern.compile(s"^${name.toLowerCase}")))
        .extend(region.isDefined)
        (_.extend(region)(region => _.and("c.a.rid").is(region.id)),
            _.extend(country)(country => _.and("c.a.cid").is(country.id)))
        .extend(gender)(g => _.and("g").is(g.id))
        .extend(maritalstatus)(ms => _.and("fs").is(ms))
        .and("star").exists(false)
      )
  }

  def searchUsers(request: SearchUsersRequest): Seq[User] = {
    val query = searchUsersQuery(request).withPaging(request.offsetLimit)
    query.fields().exclude("ns")
    mongo.find(query, entityClass).asScala.toSeq
  }

  def searchUsersCount(request: SearchUsersRequest): Int = {
    val query = searchUsersQuery(request)
    mongo.count(query, entityClass).toInt
  }

  private def searchStarsQuery(request: SearchStarsRequest) = {
    import BuilderHelper._

    Query.query(new Criteria()
      .extend(request.name)(name => _.and("ns").in(Pattern.compile(s"^${name.toLowerCase}")))
      .extend(request.country)(country => _.and("c.a.cid").is(country.id))
      .extend(request.category)(category => _.and("star.sc").in(category.id))
      .andOperator(starCriteria)
    )
  }

  def searchStars(request: SearchStarsRequest, offsetLimit: OffsetLimit): Seq[User] = {
    import BuilderHelper._
    import com.tooe.core.util.ProjectionHelper._

    val sortField = request.sort.map(_.field).getOrElse(StarSort.Name.field)

    def sortingCriteria = new Sort(Sort.Direction.DESC, sortField)

    def projectionFields = request.fields map (_ map starParamsToMongoFieldsNames)

    val query = searchStarsQuery(request)
      .withPaging(offsetLimit).sort(sortingCriteria)
      .extend(projectionFields)(fields => _.extendProjection(fields + sortField))
    query.fields.exclude("ns")
    mongo.find(query, entityClass).asScala.toSeq
  }

  def searchStarsCount(request: SearchStarsRequest): Long = {

    val query = searchStarsQuery(request)

    mongo.count(query, entityClass)
  }

  def addGift(userId: UserId, presentId: PresentId) = {
    val q = Query.query(new Criteria("_id").is(userId.id))
    val u = new Update().push("gs", presentId.id)
    mongo.updateFirst(q, u, entityClass).asUpdateResult
  }

  def findAbsentIds(userIds: Seq[UserId]) = {
    val existIds = repo.existedUserIds(userIds map (_.id)).asScala map (_.id)
    (userIds.toSet -- existIds).toSeq
  }

  def addPhotoToUser(userId: UserId, photoId: PhotoId) {
    val query = Query.query(new Criteria("_id").is(userId.id))
    val update = (new Update).push("lp", new BasicDBObject("$each", java.util.Arrays.asList(photoId.id)).append("$slice", -6) )
    mongo.updateFirst(query, update, entityClass)
  }

  def updateUserLastPhotos(userId: UserId, photoIds: Seq[PhotoId]) = {
    val query = Query.query(new Criteria("_id").is(userId.id))
    val update = (new Update).set("lp", photoIds map (_.id))
    mongo.updateFirst(query, update, entityClass)
  }

  def getUserMedia(userId: UserId) = repo.getUserMedia(userId.id)

  def updateUser(userId: UserId, fields: UpdateFields) {
    import DBObjectConverters._

    implicit val PhoneShortConverter = new DBObjectConverter[PhoneShort] {
      def serializeObj(obj: PhoneShort) = DBObjectBuilder()
        .field("c").value(obj.code)
        .field("n").value(obj.number)

      def deserializeObj(source: DBObjectExtractor) = PhoneShort(
        code = source.field("c").value[String],
        number = source.field("n").value[String]
      )
    }

    val query = Query.query(new Criteria("_id").is(userId.id))
    val update = (new Update)
      .setSkipUnset("n",fields.name)
      .setSkipUnset("ln", fields.lastName)
      .setSkipUnset("c.e", fields.email)
      .setSkipUnset("c.a.cid", fields.countryId.map(_.id))
      .setSkipUnset("c.a.rid", fields.regionId.map(_.id))
      .setSkipUnset("c.a.r", fields.regionName)
      .setSkipUnset("c.a.co", fields.countryName)
      .setSkipUnset("bd", fields.birthday)
      .setSkipUnset("g", fields.gender.map(_.id))
      .setSkipUnset("fs", fields.maritalStatus.map(_.id))
      .setSkipUnset("c.p.mf", fields.phones.main)
      .setSkipUnsetSeq("c.p.f", fields.phones.all)
      .setSkipUnset("d.e", fields.education)
      .setSkipUnset("d.job", fields.job)
      .setSkipUnset("d.am", fields.aboutMe)
      .setSkipUnset("s.pr", fields.settings.map(_.pageRights))
      .setSkipUnset("s.mr", fields.settings.map(_.mapRights))
      .setSkipUnset("s.ms.st", fields.messageSettings.map(_.showText))
      .setSkipUnset("s.ms.a", fields.messageSettings.map(_.playAudio))
      .setSkipUnset("s.ms.se.e", fields.messageSettings.map(_.sendEmail.map(_.email)))
      .setSkipUnsetSeq("s.ms.se.ev", fields.messageSettings.map(_.sendEmail.map(_.events)))
      .setSkipUnset("os", fields.onlineStatus)

    if (update.nonEmpty) {
      mongo.updateFirst(query, update, entityClass)
      //this is done to update namesearch array
      if (!(fields.name orElse fields.lastName).isEmpty) {
        query.fields.exclude("ns")
        mongo.save(mongo.findOne(query, entityClass))
      }
    }
  }

  def getUserStatistics(userId: UserId) = repo.getUserStatistics(userId.id).statistics

  def addPhotoAlbum(userId: UserId, id: PhotoAlbumId): Unit = {
    val query = Query.query(new Criteria("_id").is(userId.id))
    val update = new Update().push("pa", id.id)
    mongo.updateFirst(query, update, entityClass)
  }

  def removePhotoAlbum(userId: UserId, id: PhotoAlbumId): Unit = {
    val query = Query.query(new Criteria("_id").is(userId.id))
    val update = new Update().pull("pa", id.id)
    mongo.updateFirst(query, update, entityClass)
  }

  def addWish(userId: UserId, wishId: WishId): Unit = {
    val query = Query.query(new Criteria("_id").is(userId.id))
    val update = new Update().push("ws", wishId.id)
    mongo.updateFirst(query, update, entityClass)
  }

  def removeWish(userId: UserId, wishId: WishId): Unit = {
    val query = Query.query(new Criteria("_id").is(userId.id))
    val update = new Update().pull("ws", wishId.id)
    mongo.updateFirst(query, update, entityClass)
  }

  private[this] def searchUserQuery(userIds: Seq[UserId], request: SearchUserFriendsRequest): Query = {
    import BuilderHelper._
    Query.query(
      new Criteria("_id").in(userIds.map(_.id).asJavaCollection)
        .extend(request.name)(name => _.and("ns").in(Pattern.compile(s"^${name.toLowerCase}")))
    )
  }

  def searchUserFriends(usersIds: Seq[UserId], request: SearchUserFriendsRequest, offsetLimit: OffsetLimit) = {
    val query = searchUserQuery(usersIds, request).withPaging(offsetLimit)
    query.fields.exclude("ns")
    mongo.find(query, entityClass).asScala
  }

  def countSearchUserFriends(usersIds: Seq[UserId], request: SearchUserFriendsRequest) =
    mongo.count(searchUserQuery(usersIds, request), entityClass)

  implicit def updateHelper(update: Update) = new {
    def increaseOptLock: Update = update.inc("ol", 1)
  }

  def updateUserMedia(userId: UserId, userMedia: Seq[UserMedia], optimisticLock: Option[Int]): UpdateResult = {
    import BuilderHelper._
    val query = Query.query(new Criteria("_id").is(userId.id).extend(optimisticLock)(ol => _.and("ol").is(ol)))
    val update = new Update().setSerializeSeq("um", userMedia).increaseOptLock
    mongo.updateFirst(query, update, entityClass).asUpdateResult
  }

  def updateMediaStorageToS3(userId: UserId, media: MediaObjectId, newMedia: MediaObjectId) {
    val query = Query.query(new Criteria("_id").is(userId.id).and("um.u.mu").is(media.id))
    val update = new Update().set("um.$.u.t", UrlType.s3.id).set("um.$.u.mu", newMedia.id).increaseOptLock
    mongo.updateFirst(query, update, entityClass)
  }

  def updateMediaStorageToCDN(userId: UserId, media: MediaObjectId) {
    val query = Query.query(new Criteria("_id").is(userId.id).and("um.u.mu").is(media.id))
    val update = new Update().unset("um.$.u.t").increaseOptLock
    mongo.updateFirst(query, update, entityClass)
  }

  def findTopStars = {
    val query = new Query().withPaging(OffsetLimit(0, topStarsCount)).desc("star.suc")
    query.fields().exclude("ns")
    mongo.find(query, entityClass).asScala
  }
}