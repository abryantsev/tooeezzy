package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import java.util.Date
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain._
import com.tooe.core.domain.CompanyId
import com.tooe.core.domain.AdminUserId
import com.tooe.core.db.mongo.domain.CompanyContract
import com.tooe.core.db.mongo.domain.CompanyInformation
import com.tooe.core.db.mongo.domain.CompanyMedia
import com.tooe.core.db.mongo.domain.CompanyContact
import com.tooe.core.db.mongo.domain.CompanyManagement

@WritingConverter
class PreModerationCompanyWriteConverter extends Converter[PreModerationCompany, DBObject] with PreModerationCompanyConverter {
  def convert(source: PreModerationCompany) = preModerationCompanyConverter.serialize(source)
}

@ReadingConverter
class PreModerationCompanyReadConverter extends Converter[DBObject, PreModerationCompany] with PreModerationCompanyConverter {
  def convert(source: DBObject) = preModerationCompanyConverter.deserialize(source)
}

trait PreModerationCompanyConverter extends CompanyContractConverter
                       with CompanyContactConverter
                       with CompanyManagementConverter
                       with CompanyInformationConverter
                       with CompanyMediaConverter
                       with PreModerationStatusConverter
                       with PaymentCompanyConverter {

  import DBObjectConverters._

  implicit val preModerationCompanyConverter = new DBObjectConverter[PreModerationCompany] {

    def serializeObj(obj: PreModerationCompany) = DBObjectBuilder()
      .id.value(obj.id)
      .field("aid").value(obj.agentId)
      .field("mid").value(obj.mainUserId)
      .field("rt").value(obj.registrationDate)
      .field("n").value(obj.name)
      .field("sn").value(obj.shortName)
      .field("d").value(obj.description)
      .field("su").value(obj.subject)
      .field("co").value(obj.contract)
      .field("c").value(obj.contact)
      .field("m").value(obj.management)
      .field("l").value(obj.legalInformation)
      .field("cm").value(obj.companyMedia)
      .field("mod").value(obj.moderationStatus)
      .field("p").value(obj.payment)
      .field("pt").value(obj.partnershipType)
      .field("puid").value(obj.publishCompany)
      .field("mod").value(obj.moderationStatus)

    def deserializeObj(source: DBObjectExtractor) = PreModerationCompany(
      id = source.id.value[PreModerationCompanyId],
      agentId = source.field("aid").value[AdminUserId],
      mainUserId = source.field("mid").value[AdminUserId],
      registrationDate = source.field("rt").value[Date],
      name = source.field("n").value[String],
      shortName = source.field("sn").opt[String],
      description = source.field("d").value[String],
      subject = source.field("su").opt[String],
      contract = source.field("co").value[CompanyContract],
      contact = source.field("c").value[CompanyContact],
      management = source.field("m").value[CompanyManagement],
      legalInformation = source.field("l").value[CompanyInformation],
      companyMedia = source.field("cm").seq[CompanyMedia],
      moderationStatus = source.field("mod").value[PreModerationStatus],
      payment = source.field("p").value[CompanyPayment],
      partnershipType = source.field("pt").value[PartnershipType],
      publishCompany = source.field("puid").opt[CompanyId]
    )
  }

}
