package com.tooe.core.service

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.PromotionRepository
import com.tooe.core.domain.PromotionId
import com.tooe.core.db.mongo.domain.{MapKey, Promotion}
import com.tooe.core.usecase.{PromotionChangeRequest, SearchPromotionsRequest}
import com.tooe.core.usecase.promotion.PromotionSearchSortType
import scala.collection.JavaConverters._
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import com.tooe.core.util.{Lang, BuilderHelper}
import com.tooe.core.db.mongo.query._
import com.tooe.core.db.mongo.converters.{DBCommonConverters, DBObjectConverters}

trait PromotionDataService {
  def save(entity: Promotion): Promotion
  def findOne(id: PromotionId): Option[Promotion]
  def incrementVisitorsCounter(id: PromotionId, amount: Int)
  def exists(id: PromotionId): Boolean
  def searchPromotions(spr:SearchPromotionsRequest, lang: String): Seq[Promotion]
  def searchPromotionsCount(request: SearchPromotionsRequest, lang: String): Long
  def find(ids: Set[PromotionId]): Seq[Promotion]
  def update(id: PromotionId, request: PromotionChangeRequest, lang: Lang): Unit
}

@Service
class PromotionDataServiceImpl extends PromotionDataService {
  @Autowired var repo: PromotionRepository = _
  @Autowired var mongo: MongoTemplate = _

  import DBObjectConverters._
  import DBCommonConverters._
  import BuilderHelper._

  def save(entity: Promotion) = repo.save(entity)

  def findOne(id: PromotionId) = Option(repo.findOne(id.id))

  def incrementVisitorsCounter(id: PromotionId, amount: Int) {
    mongo.updateFirst(
      Query.query(new Criteria("id").is(id.id)),
      (new Update).inc("vc", amount),
      classOf[Promotion]
    )
  }

  def exists(id: PromotionId) = repo.exists(id.id)

  def searchPromotions(request: SearchPromotionsRequest, lang: String) = {
    val sortingCriteria = request.sort.getOrElse(PromotionSearchSortType.None).getSort(MapKey("n", lang).key)  // TODO isFavorite not used
    val result = mongo.find(
      searchPromotionsQuery(request, lang).withPaging(request.offsetLimit).sort(sortingCriteria),
      classOf[Promotion]
    )
    result.asScala.toSeq
  }

  def searchPromotionsCount(request: SearchPromotionsRequest, lang: String) =
    mongo.count(
      searchPromotionsQuery(request, lang),
      classOf[Promotion]
    )

  private def searchPromotionsQuery(request: SearchPromotionsRequest, lang: String) = Query.query(
    new Criteria("l.rid").is(request.regionId.id)
      .extend(request.categoryId)(categoryId => _.and("l.lc").all(categoryId))
      .extend(request.name)(name => _.and(MapKey("n", lang)).regex(name))
      .extend(request.date)(date => _.and("ds.st").lte(date).and("ds.et").gte(date))
  )

  def find(ids: Set[PromotionId]): Seq[Promotion] = repo.findByIds(ids.map(_.id).toSeq).asScala

  def update(id: PromotionId, request: PromotionChangeRequest, lang: Lang) {
    val query = Query.query(new Criteria("id").is(id.id))
    val update = (new Update).setOrSkip(LocalizedField("n", lang).value, request.name)
                            .setOrSkip(LocalizedField("d", lang).value, request.description)
                            .setOrSkip(LocalizedField("i", lang).value, request.additionalInformation)
                            .setOrSkip("ds.st", request.startDate)
                            .setSkipUnset("ds.et", request.endDate)
                            .setSkipUnset("ds.at", request.time)
                            .setSkipUnset("ds.p", request.period)
                            .setOrSkip(LocalizedField("pc", lang).value, request.price)
                            .setOrSkipSeq("pm", request.media.map(Seq(_)))
    mongo.updateFirst(query, update, classOf[Promotion])
  }
}