package com.tooe.core.service

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.junit.{AfterClass, BeforeClass}
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import junit.framework.Assert.fail

@RunWith(classOf[Suite])
@SuiteClasses(Array(
classOf[CacheSessionDataServiceTest],
classOf[CheckinDataServiceTest],
classOf[CountryDataServiceTest],
classOf[CredentialsDataServiceTest],
classOf[InfoMessageDataServiceTest],
classOf[LocationCategoryDataServiceTest],
classOf[LocationDataServiceTest],
classOf[LocationPhotoAlbumDataServiceTest],
classOf[LocationPhotoDataServiceTest],
classOf[LocationPhotoLikeDataServiceTest],
classOf[LocationPhotoCommentDataServiceTest],
classOf[MaritalStatusDataServiceTest],
classOf[PhotoAlbumDataServiceTest],
classOf[PhotoDataServiceTest],
classOf[PhotoLikeDataServiceTest],
classOf[PhotoCommentDataServiceTest],
classOf[ProductDataServiceTest],
classOf[PromotionDataServiceTest],
classOf[PromotionVisitorDataServiceTest],
classOf[RegionDataServiceTest],
classOf[SaleDataServiceTest],
classOf[UserDataServiceTest],
classOf[UserEventDataServiceTest],
classOf[UserPhoneDataServiceTest],
classOf[WishDataServiceTest],
classOf[WishLikeDataServiceTest],
classOf[LocationSubscriptionDataServiceTest],
classOf[LocationNewsDataServiceTest],
classOf[PresentDataServiceTest],
classOf[StarsCategoriesDataServiceTest],
classOf[StarSubscriptionDataServiceTest],
classOf[CompanyDataServiceTest],
classOf[CacheUserOnlineDataServiceTest],
classOf[AdminUserDataServiceTest],
classOf[AdminCredentialsDataServiceTest],
classOf[EventTypeDataServiceTest],
classOf[CacheAdminSessionDataServiceTest],
classOf[LocationSubscriptionDataServiceTest],
classOf[LocationNewsLikeDataServiceTest],
classOf[AdminUserEventDataServiceTest],
classOf[PreModerationCompanyDataServiceTest],
classOf[LocationsChainDataServiceTest],
classOf[UrlsDataServiceTest],
classOf[CalendarEventDataServiceTest]
))
object MongoDataServiceSuite {

  @BeforeClass
  def before {
    val helper = new MongoDaoHelper("")
    try {
      helper.mongodb.getLastError
    } catch {
      case e: Exception => fail("MongoDB is unavailable: "+e.getMessage)
    }
  }

  @AfterClass
  def after {
  }
}
