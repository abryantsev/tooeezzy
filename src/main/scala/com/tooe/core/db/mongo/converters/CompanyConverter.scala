package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import java.util.Date
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain._

@WritingConverter
class CompanyWriteConverter extends Converter[Company, DBObject] with CompanyConverter {
  def convert(source: Company) = companyConverter.serialize(source)
}

@ReadingConverter
class CompanyReadConverter extends Converter[DBObject, Company] with CompanyConverter {
  def convert(source: DBObject) = companyConverter.deserialize(source)
}

trait CompanyConverter extends CompanyContractConverter
                       with CompanyContactConverter
                       with CompanyManagementConverter
                       with CompanyInformationConverter
                       with CompanyMediaConverter
                       with PaymentCompanyConverter {

  import DBObjectConverters._

  implicit val companyConverter = new DBObjectConverter[Company] {

    def serializeObj(obj: Company) = DBObjectBuilder()
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
      .field("p").value(obj.payment)
      .field("pt").value(obj.partnershipType)
      .field("st").value(obj.synchronizationTime)
      .field("ut").value(obj.updateTime)

    def deserializeObj(source: DBObjectExtractor) = Company(
      id = source.id.value[CompanyId],
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
      payment = source.field("p").value[CompanyPayment],
      partnershipType = source.field("pt").value[PartnershipType],
      synchronizationTime = source.field("st").opt[Date],
      updateTime = source.field("ut").opt[Date]
    )
  }

}
