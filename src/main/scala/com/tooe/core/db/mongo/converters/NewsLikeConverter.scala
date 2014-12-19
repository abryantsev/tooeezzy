package com.tooe.core.db.mongo.converters
import com.tooe.core.db.mongo.domain._
import com.tooe.core.db.mongo.domain.NewsLike
import com.tooe.core.domain.{UserId, NewsId}
import java.util.Date
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject

@WritingConverter
class NewsLikeWriteConverter extends Converter[NewsLike, DBObject] with NewsLikeConverter {
  def convert(source: NewsLike) = newsLikeConverter.serialize(source)
}

@ReadingConverter
class NewsLikeReadConverter extends Converter[DBObject, NewsLike] with NewsLikeConverter {
  def convert(source: DBObject) = newsLikeConverter.deserialize(source)
}

trait NewsLikeConverter {

  import DBObjectConverters._

  implicit val newsLikeConverter = new DBObjectConverter[NewsLike] {

    def serializeObj(obj: NewsLike) = DBObjectBuilder()
      .id.value(obj.id)
      .field("nid").value(obj.newsId)
      .field("t").value(obj.time)
      .field("uid").value(obj.userId)

    def deserializeObj(source: DBObjectExtractor) = NewsLike(
      id = source.id.value[NewsLikeId],
      newsId = source.field("nid").value[NewsId],
      time = source.field("t").value[Date],
      userId = source.field("uid").value[UserId]
    )

  }
}
