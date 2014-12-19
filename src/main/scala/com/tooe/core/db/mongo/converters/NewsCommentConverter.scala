package com.tooe.core.db.mongo.converters

import com.tooe.core.domain._
import java.util.Date
import com.tooe.core.domain.NewsCommentId
import com.tooe.core.domain.UserId
import com.tooe.core.db.mongo.domain.NewsComment
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject

@WritingConverter
class NewsCommentWriteConverter extends Converter[NewsComment, DBObject] with NewsCommentConverter {

  def convert(source: NewsComment) = newsCommentConverter
    .serialize(source)

}

@ReadingConverter
class NewsCommentReadConverter extends Converter[DBObject, NewsComment] with NewsCommentConverter {

  def convert(source: DBObject) = newsCommentConverter.deserialize(source)

}


trait NewsCommentConverter {

  import DBObjectConverters._

  implicit val newsCommentConverter = new DBObjectConverter[NewsComment] {
    def serializeObj(obj: NewsComment) = DBObjectBuilder()
      .id.value(obj.id)
      .field("pid").value(obj.parentId)
      .field("nid").value(obj.newsId)
      .field("t").value(obj.creationDate)
      .field("m").value(obj.message)
      .field("aid").value(obj.authorId)

    def deserializeObj(source: DBObjectExtractor) = NewsComment(
      id = source.id.value[NewsCommentId],
      parentId = source.field("pid").opt[NewsCommentId],
      newsId = source.field("nid").value[NewsId],
      creationDate = source.field("t").value[Date],
      message = source.field("m").value[String],
      authorId = source.field("aid").value[UserId]
    )
  }
}
