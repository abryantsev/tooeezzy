package com.tooe.core.db.mongo.util

import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.core.{JsonParser, JsonGenerator, Version}
import com.fasterxml.jackson.databind.module.SimpleModule
import org.bson.types.ObjectId
import spray.httpx.marshalling.Marshaller
import spray.http._
import reflect.ClassTag
import spray.httpx.unmarshalling._
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.tooe.core.usecase.{PaymentCompleteStatus, payment}
import com.tooe.api.validation.{ValidationResult, ValidationFailed, ValidationException, ValidationHelper}
import java.util.{Calendar, Date}
import com.tooe.core.db.mongo.domain.WishPresentStatus
import com.tooe.core.domain._
import com.tooe.core.marshalling.UnsetableModule
import com.tooe.core.db.graph.domain.FriendshipType
import com.tooe.core.usecase.location.LocationSearchViewType
import com.tooe.core.usecase.product.{ProductSearchView, ProductSearchSortType}
import java.net.URLDecoder
import spray.http.HttpEntity.NonEmpty

object JacksonModuleScalaSupport extends JacksonModuleScalaSupport

trait JacksonModuleScalaSupport extends CSVDeserializer {

  implicit def moduleEnhancer(mongoModule: SimpleModule) = new {
    def hasIdConverter[T <: HasIdentity : ClassTag](factoryParam: HasIdentityFactory[T]) {
      val deserializer = new HasIdentityDeserializer[T] {
        def factory: HasIdentityFactory[T] = factoryParam
      }
      mongoModule.addDeserializer(reflect.classTag[T].runtimeClass.asInstanceOf[Class[T]], deserializer)
    }

    def stringIdConverter[T <: AnyRef : ClassTag](dserFun: String => T)(serFun: T => String): Unit = {
      val clazz = reflect.classTag[T].runtimeClass.asInstanceOf[Class[T]]
      mongoModule.addSerializer(clazz, new IdTypeSerializer[T](serFun))
      mongoModule.addDeserializer(clazz, new IdTypeDeserializer[T](dserFun))
    }

    def objectIdConverter[T <: AnyRef : ClassTag](dserFun: ObjectId => T)(serFun: T => ObjectId): Unit =
      stringIdConverter[T](dserFun compose ((s: String) => new ObjectId(s)))(serFun andThen (_.toString))

    def intIdConverter[T <: AnyRef : ClassTag](dserFun: Int => T)(serFun: T => Int): Unit = {
      val clazz = reflect.classTag[T].runtimeClass.asInstanceOf[Class[T]]
      mongoModule.addSerializer(clazz, new IntTypeSerializer[T](serFun))
      mongoModule.addDeserializer(clazz, new IntTypeDeserializer[T](dserFun))
    }
    def enumConverter[T <: AnyRef : ClassTag](serFun: T => String): Unit = {
      val clazz = reflect.classTag[T].runtimeClass.asInstanceOf[Class[T]]
      mongoModule.addSerializer(clazz, new EnumTypeSerializer[T](serFun))
    }

  }

  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.registerModule(UnsetableModule)
  mapper.setSerializationInclusion(Include.NON_NULL)

  val mongoModule = new SimpleModule("MongoModule", new Version(1, 0, 0, null)) //TODO extract module out of here
  mongoModule.addSerializer(classOf[ObjectId], new ObjectIdSerializer())
  mongoModule.addSerializer(classOf[HasIdentity], new HasIdentityTypeSerializer())

  mongoModule.hasIdConverter(Gender)
  mongoModule.enumConverter[FriendshipType](_.toString.toLowerCase)
  mongoModule.hasIdConverter(PromotionStatus)
  mongoModule.hasIdConverter(PromotionPeriod)
  mongoModule.hasIdConverter(payment.ResponseType)
  mongoModule.hasIdConverter(PaymentCompleteStatus)
  mongoModule.hasIdConverter(WishPresentStatus)
  mongoModule.hasIdConverter(UserGroupType)
  mongoModule.hasIdConverter(FriendshipStatus)
  mongoModule.stringIdConverter(ProductTypeId(_))(_.id)
  mongoModule.hasIdConverter(UserEventStatus)
  mongoModule.objectIdConverter(LocationId)(_.id)
  mongoModule.objectIdConverter(ProductId)(_.id)
  mongoModule.objectIdConverter(UserId)(_.id)
  mongoModule.objectIdConverter(RegionId)(_.id)
  mongoModule.stringIdConverter(CurrencyId(_))(_.id)
  mongoModule.stringIdConverter(UsersGroupId(_))(_.id)
  mongoModule.intIdConverter(Percent(_))(_.value)
  mongoModule.objectIdConverter(PhotoId)(_.id)
  mongoModule.objectIdConverter(PhotoAlbumId)(_.id)
  mongoModule.stringIdConverter(LocationCategoryId)(_.id)
  mongoModule.objectIdConverter(CheckinId)(_.id)
  mongoModule.objectIdConverter(AdditionalLocationCategoryId)(_.id)
  mongoModule.stringIdConverter(CountryId)(_.id)
  mongoModule.objectIdConverter(PhotoLikeId)(_.id)
  mongoModule.objectIdConverter(PhotoCommentId)(_.id)
  mongoModule.objectIdConverter(LocationPhotoAlbumId)(_.id)
  mongoModule.objectIdConverter(LocationPhotoId)(_.id)
  mongoModule.objectIdConverter(PromotionId)(_.id)
  mongoModule.objectIdConverter(UserEventId)(_.id)
  mongoModule.stringIdConverter(UserEventTypeId(_))(_.id)
  mongoModule.objectIdConverter(LocationPhotoLikeId)(_.id)
  mongoModule.stringIdConverter(MaritalStatusId)(_.id)
  mongoModule.objectIdConverter(LocationNewsId)(_.id)
  mongoModule.objectIdConverter(PresentId)(_.id)
  mongoModule.objectIdConverter(WishId)(_.id)
  mongoModule.stringIdConverter(StarCategoryId)(_.id)
  mongoModule.objectIdConverter(CompanyId)(_.id)
  mongoModule.objectIdConverter(PhotoCommentId)(_.id)
  mongoModule.objectIdConverter(LocationPhotoCommentId)(_.id)
  mongoModule.objectIdConverter(AdminUserId)(_.id)
  mongoModule.hasIdConverter(PartnershipType)
  mongoModule.hasIdConverter(CompanyType)
  mongoModule.hasIdConverter(PaymentPeriod)
  mongoModule.objectIdConverter(FriendshipRequestId)(_.id)
  mongoModule.stringIdConverter(AdminRoleId(_))(_.id)
  mongoModule.hasIdConverter(ExportStatus)
  mongoModule.objectIdConverter(PreModerationCompanyId)(_.id)
  mongoModule.stringIdConverter(ModerationStatusId(_))(_.id)
  mongoModule.stringIdConverter(PeriodId(_))(_.id)
  mongoModule.stringIdConverter(PresentStatusId(_))(_.id)
  mongoModule.objectIdConverter(NewsId)(_.id)
  mongoModule.objectIdConverter(NewsCommentId)(_.id)
  mongoModule.objectIdConverter(LocationsChainId)(_.id)
  mongoModule.hasIdConverter(LocationSearchViewType)
  mongoModule.hasIdConverter(ProductSearchSortType)
  mongoModule.hasIdConverter(ProductSearchView)
  mongoModule.objectIdConverter(PreModerationLocationId)(_.id)
  mongoModule.stringIdConverter(NewsTypeId(_))(_.id)
  mongoModule.objectIdConverter(CalendarEventId)(_.id)
  mongoModule.stringIdConverter(OnlineStatusId(_))(_.id)
  mongoModule.stringIdConverter(MediaObjectId)(_.id)
  mongoModule.objectIdConverter(AdminUserEventId)(_.id)

  mongoModule.addSerializer(classOf[Date], new DateSerializer)
  mongoModule.addDeserializer(classOf[Date], new DateDeserializer)

  mongoModule.addSerializer(classOf[BigDecimal], new DecimalSerializer)
  mongoModule.addDeserializer(classOf[BigDecimal], new DecimalDeserializer)

  mongoModule.addSerializer(classOf[BigInt], new BigIntSerializer)
  mongoModule.addDeserializer(classOf[BigInt], new BigIntDeserializer)

  mapper.setSerializationInclusion(Include.NON_NULL)
  mapper.registerModule(mongoModule)

  def serialize(obj:Object) = mapper.writeValueAsString(obj)
  def deserialize[T](obj:String)(implicit m: ClassTag[T]) = mapper.readValue(obj, m.runtimeClass.asInstanceOf[Class[T]])

  val MarshallerContentType = MediaTypes.`application/json`

  implicit def marshaller[T <: AnyRef] =
    Marshaller.of[T](ContentType(MarshallerContentType, HttpCharsets.`UTF-8`)) { (value, contentType, ctx) =>
      ctx.marshalTo(HttpEntity(ContentType(MarshallerContentType, HttpCharsets.`UTF-8`), mapper.writeValueAsBytes(value)))
    }

  implicit def unmarshaller[T <: UnmarshallerEntity : ClassTag] =
    unmarshallerWithValidation[T](ContentTypeRange(MarshallerContentType)) { case NonEmpty(contentType, data) =>
      val content = data.asString
      deserialize[T](content)
    }

  private def unmarshallerWithValidation[T](unmarshalFrom: ContentTypeRange*)(f: PartialFunction[HttpEntity, T]): Unmarshaller[T] =
    new SimpleUnmarshaller[T] {
      val canUnmarshalFrom = unmarshalFrom
      def unmarshal(entity: HttpEntity) =
        if (f.isDefinedAt(entity)) {
          val r = protect(f(entity))
          r match {
            case Right(o: AnyRef) => ValidationHelper.validateRequest(o)
            case _ =>
          }
          r
        }
        else Left(ContentExpected)
    }
}

trait CSVDeserializer {

  case class CSV[T](values: Seq[T]) {
    def toSeq: Seq[T] = values
    def toSet: Set[T] = values.toSet
  }

  object CSV {
    implicit def toSet[T](value: CSV[T]): Set[T] = value.toSet
    implicit def toSetOpt[T](value: Option[CSV[T]]): Option[Set[T]] = value map (_.toSet)
  }

  implicit def valueSetUnmarshaller[T : FromStringDeserializer] = new Deserializer[String, CSV[T]] {
    def apply(rawValues: String): Deserialized[CSV[T]] = {
      val deserializer = implicitly[FromStringDeserializer[T]]
      val strValues = URLDecoder.decode(rawValues, "UTF-8")  split ","
      val deserializedValues: Seq[Deserialized[T]] = strValues map deserializer
      val errors: Seq[DeserializationError] = deserializedValues flatMap (_.left.toOption)
      if (errors.nonEmpty) {
        val failures: Seq[ValidationResult] = errors collect {
          case MalformedContent(message, _) => ValidationFailed(message)
        }
        val validationFailed = failures reduce (_ & _)
        /*
         Can't just return Left(_: DeserializationError) because it'll let Spray try other routes which is not
         desirable behavior in most cases, generally we want to get what value is not correct and can't be deserialized.
         */
        throw ValidationException(validationFailed.asInstanceOf[ValidationFailed], StatusCodes.BadRequest)
      } else {
        val values: Seq[T] = deserializedValues flatMap (_.right.toOption)
        Right(CSV(values))
      }
    }
  }
}

/**
 * Marker interface that orders to use generic unmarshaller for entities that use generic deserialization mechanism via jackson
 * and not to interfere with the other unmarshallers for AnyRef derivatives like String and so on
 */
trait UnmarshallerEntity

trait HasIdentity {
  def id: String
}

trait HasIdentityFactory[T] {
  def get(id: String): Option[T]

  implicit val deserializer = new Deserializer[String, T] {
    def apply(value: String): Either[DeserializationError, T] =
      get(value) toRight MalformedContent(s"'$value' is not a valid value of "+className)
  }

  private def className = getClass.getSimpleName
}

trait HasIdentityFactoryEx[T <: HasIdentity] extends HasIdentityFactory[T] {
  def values: Seq[T]
  lazy val valueById: Map[String, T] = values.map(x => x.id -> x).toMap
  def get(id: String) = valueById.get(id)

  implicit val hasIdentityFactoryExDeserializer = new Deserializer[String, T] {
    def apply(value: String): Either[DeserializationError, T] = {
      val allowedValues = values.map(_.id).mkString("[", ", ", "]")
      get(value) toRight MalformedContent(s"'$value' is not a valid value. Allowed values: $allowedValues")
    }
  }
}

class ObjectIdSerializer extends JsonSerializer[ObjectId] {
  def serialize(p1: ObjectId, p2: JsonGenerator, p3: SerializerProvider) { p2.writeString(p1.toString)}
}

class HasIdentityTypeSerializer extends JsonSerializer[HasIdentity] {
  def serialize(value: HasIdentity, jgen: JsonGenerator, provider: SerializerProvider) {
    jgen.writeString(value.id)
  }
}

class IdTypeSerializer[T <: AnyRef](strId: T => String) extends JsonSerializer[T] {
  def serialize(value: T, jgen: JsonGenerator, provider: SerializerProvider) {
    jgen.writeString(strId(value))
  }
}

class IdTypeDeserializer[T <: AnyRef](idStr: String => T) extends JsonDeserializer[T] {
  def deserialize(jp: JsonParser, ctxt: DeserializationContext): T = {
    val id = jp.getText.trim
    idStr(id)
  }
}

class IntTypeSerializer[T <: AnyRef](strId: T => Int) extends JsonSerializer[T] {
  def serialize(value: T, jgen: JsonGenerator, provider: SerializerProvider) {
    jgen.writeNumber(strId(value))
  }
}

class IntTypeDeserializer[T <: AnyRef](idInt: Int => T) extends JsonDeserializer[T] {
  def deserialize(jp: JsonParser, ctxt: DeserializationContext): T = {
    val id = jp.getIntValue
    idInt(id)
  }
}
class DecimalSerializer extends JsonSerializer[BigDecimal] {
  def serialize(value: BigDecimal, jgen: JsonGenerator, provider: SerializerProvider) {
    jgen.writeNumber(value.bigDecimal)
  }
}

class DecimalDeserializer extends JsonDeserializer[BigDecimal] {
  def deserialize(jp: JsonParser, ctxt: DeserializationContext): BigDecimal = {
    jp.getDecimalValue
  }
}
class BigIntSerializer extends JsonSerializer[BigInt] {
  def serialize(value: BigInt, jgen: JsonGenerator, provider: SerializerProvider) {
    jgen.writeNumber(value.bigInteger)
  }
}

class BigIntDeserializer extends JsonDeserializer[BigInt] {
  def deserialize(jp: JsonParser, ctxt: DeserializationContext): BigInt = {
    jp.getBigIntegerValue
  }
}
class EnumTypeSerializer[T](strId: T => String) extends JsonSerializer[T] {
  def serialize(value: T, jgen: JsonGenerator, provider: SerializerProvider) {
    jgen.writeString(strId(value))
  }
}

trait HasIdentityDeserializer[T <: AnyRef] extends JsonDeserializer[T] {
  def factory: HasIdentityFactory[T]

  def deserialize(jp: JsonParser, ctxt: DeserializationContext): T = {
    val id = jp.getText.trim
    factory.get(id) getOrElse (throw new IllegalArgumentException("unknown entity id="+id))
  }
}

class DateSerializer extends JsonSerializer[Date] {
  def serialize(value: Date, jgen: JsonGenerator, provider: SerializerProvider) {
    val cal = Calendar.getInstance()
    cal.setTime(value)
    val secondsSince1970 = cal.getTimeInMillis / 1000
    jgen.writeNumber(secondsSince1970)
  }
}

class DateDeserializer extends JsonDeserializer[Date] {
  def deserialize(jp: JsonParser, ctxt: DeserializationContext): Date = {
    val secondsSince1970 = jp.getLongValue
    val cal = Calendar.getInstance()
    cal.setTimeInMillis(secondsSince1970 * 1000)
    cal.getTime
  }
}