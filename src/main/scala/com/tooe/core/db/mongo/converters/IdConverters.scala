package com.tooe.core.db.mongo.converters

import com.tooe.core.domain._
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.util.{HasIdentity, HasIdentityFactoryEx}
import com.tooe.core.db.mongo.domain.{LocationSpecialRole, LocationsChainStatsId, FavoriteStatsId, NewsLikeId}

trait IdConverters extends ObjectIdConverters with StringIdConverters with HasIdentityConverters

trait ObjectIdConverters {
  implicit val PresentIdConverter = converter(PresentId)(_.id)
  implicit val WishIdConverter = converter(WishId)(_.id)
  implicit val PhotoAlbumIdConverter = converter(PhotoAlbumId)(_.id)
  implicit val ProductIdConverter = converter(ProductId)(_.id)
  implicit val LocationIdConverter = converter(LocationId)(_.id)
  implicit val RegionIdConverter = converter(RegionId)(_.id)
  implicit val UserIdConverter = converter(UserId)(_.id)
  implicit val UserPhotoIdConverter = converter(UserPhoneId)(_.id)
  implicit val PhotoLikeIdConverter = converter(PhotoLikeId)(_.id)
  implicit val PhotoIdConverter = converter(PhotoId)(_.id)
  implicit val UserEventIdConverter = converter(UserEventId)(_.id)
  implicit val PromotionIdConverter = converter(PromotionId)(_.id)
  implicit val LocationPhotoLikeIdConverter = converter(LocationPhotoLikeId)(_.id)
  implicit val LocationPhotoIdConverter = converter(LocationPhotoId)(_.id)
  implicit val LocationPhotoCommentIdConverter = converter(LocationPhotoCommentId)(_.id)
  implicit val LocationSubscriptionIdConverter = converter(LocationSubscriptionId)(_.id)
  implicit val LocationNewsIdConverter = converter(LocationNewsId)(_.id)
  implicit val UserCommentIdConverter = converter(UserCommentId)(_.id)
  implicit val StarSubscriptionIdConverter = converter(StarSubscriptionId)(_.id)
  implicit val NewsIdConverter = converter(NewsId)(_.id)
  implicit val WishLikeIdConverter = converter(WishLikeId)(_.id)
  implicit val NewsCommentIdConverter = converter(NewsCommentId)(_.id)
  implicit val PromotionVisitorIdConverter = converter(PromotionVisitorId)(_.id)
  implicit val AdditionalLocationCategoryIdConverter = converter(AdditionalLocationCategoryId)(_.id)
  implicit val FriendsCacheIdConverter = converter(FriendsCacheId)(_.id)
  implicit val CheckinIdConverter = converter(CheckinId)(_.id)
  implicit val CompanyIdConverter = converter(CompanyId)(_.id)
  implicit val AdminUserIdConverter = converter(AdminUserId)(_.id)
  implicit val AdminCredentialsIdConverter = converter(AdminCredentialsId)(_.id)
  implicit val writeSnifferCacheIdConverter = converter(WriteSnifferCacheId)(_.id)
  implicit val LocationStatisticsIdConverter = converter(LocationStatisticsId)(_.id)
  implicit val LocationNewsLikeIdConverter = converter(LocationNewsLikeId)(_.id)
  implicit val FriendshipRequestIdConverter = converter(FriendshipRequestId)(_.id)
  implicit val AdminUserEventIdConverter = converter(AdminUserEventId)(_.id)
  implicit val PreModerationCompanyIdConverter = converter(PreModerationCompanyId)(_.id)
  implicit val LocationsChainIdConverter = converter(LocationsChainId)(_.id)
  implicit val LocationPhotoAlbumIdConverter = converter(LocationPhotoAlbumId)(_.id)
  implicit val NewsLikeIdConverter = converter(NewsLikeId)(_.id)
  implicit val FavoriteStatsIdConverter = converter(FavoriteStatsId)(_.id)
  implicit val LocationsChainStatsIdConverter = converter(LocationsChainStatsId)(_.id)
  implicit val PreModerationLocationIdConverter = converter(PreModerationLocationId)(_.id)
  implicit val UrlsIdConverter = converter(UrlsId)(_.id)
  implicit val CalendarEventIdConverter = converter(CalendarEventId)(_.id)

  private def converter[T](deserializeFun: ObjectId => T)(serializeFun: T => ObjectId) = new DBSimpleConverter[T] {
    def serialize(obj: T): ObjectId = serializeFun(obj)
    def deserialize(source: Any): T = deserializeFun(source.asInstanceOf[ObjectId])
  }
}

trait StringIdConverters {
  implicit val LocationSpecialRoleConverter = converter(LocationSpecialRole(_))(_.id)
  implicit val CountryIdConverter = converter(CountryId)(_.id)
  implicit val StarCategoryIdConverter = converter(StarCategoryId)(_.id)
  implicit val PresentStatusIdConverter = converter(PresentStatusId(_))(_.id)
  implicit val PresentCodeIdConverter = converter(PresentCode)(_.value)
  implicit val MaritalStatusIdConverter = converter(MaritalStatusId)(_.id)
  implicit val LocationCategoryIdConverter = converter(LocationCategoryId(_))(_.id)
  implicit val UserGroupIdConverter = converter(UsersGroupId(_))(_.id)
  implicit val CurrencyIdConverter = converter(CurrencyId(_))(_.id)
  implicit val ProductTypeIdConverter = converter(ProductTypeId(_))(_.id)
  implicit val OnlineStatusIdConverter = converter(OnlineStatusId(_))(_.id)
  implicit val LifecycleStatusIdConverter = converter(LifecycleStatusId(_))(_.id)
  implicit val AdminRoleIdConverter = converter(AdminRoleId(_))(_.id)
  implicit val ModerationStatusIdConverter = converter(ModerationStatusId(_))(_.id)
  implicit val PeriodIdConverter = converter(PeriodId(_))(_.id)
  implicit val EventTypeIdConverter = converter(EventTypeId(_))(_.id)
  implicit val UserEventTypeIdConverter = converter(UserEventTypeId(_))(_.id)
  implicit val NewsTypeIdConverter = converter(NewsTypeId(_))(_.id)
  implicit val PresentLifecycleIdConverter = converter(PresentLifecycleId(_))(_.id)
  implicit val ProductLifecycleIdConverter = converter(ProductLifecycleId(_))(_.id)
  implicit val EventGroupConverter = converter(EventGroupId(_))(_.id)
  implicit val EntityTypeConverter = converter(EntityType(_))(_.id)
  implicit val MediaObjectIdConverter = converter(MediaObjectId(_))(_.id)
  implicit val UrlTypeConverter = converter(UrlType(_))(_.id)

  private def converter[T](deserializeFun: String => T)(serializeFun: T => String) = new DBSimpleConverter[T] {
    def serialize(obj: T): String = serializeFun(obj)
    def deserialize(source: Any): T = deserializeFun(source.asInstanceOf[String])
  }
}

trait HasIdentityConverters {
  implicit val GenderConverter = converter(Gender)
  implicit val UserEventStatusConverter = converter(UserEventStatus)
  implicit val PromotionStatusConverter = converter(PromotionStatus)
  implicit val PromotionDatesPeriodConverter = converter(PromotionPeriod)
  implicit val PartnershipTypeConverter = converter(PartnershipType)
  implicit val PaymentPeriodConverter = converter(PaymentPeriod)
  implicit val CompanyTypeConverter = converter(CompanyType)

  private def converter[T <: HasIdentity](factory: HasIdentityFactoryEx[T]) = new DBSimpleConverter[T] {
    def serialize(value: T): String = value.id
    def deserialize(source: Any): T = factory.valueById(source.asInstanceOf[String])
  }
}