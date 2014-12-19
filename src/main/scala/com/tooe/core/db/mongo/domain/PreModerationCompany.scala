package com.tooe.core.db.mongo.domain

import com.tooe.core.domain._
import java.util.Date
import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.CompanyId
import com.tooe.core.domain.AdminUserId
import com.tooe.core.util.MediaHelper._

@Document(collection = "company_mod")
case class PreModerationCompany(id: PreModerationCompanyId = PreModerationCompanyId(),
                                agentId: AdminUserId,
                                mainUserId: AdminUserId,
                                registrationDate: Date = new Date,
                                name: String,
                                shortName: Option[String] = None,
                                description: String,
                                subject: Option[String],
                                contract: CompanyContract,
                                contact: CompanyContact,
                                management: CompanyManagement,
                                legalInformation: CompanyInformation,
                                companyMedia: Seq[CompanyMedia] = Nil,
                                moderationStatus: PreModerationStatus = PreModerationStatus(),
                                payment: CompanyPayment,
                                partnershipType: PartnershipType,
                                publishCompany: Option[CompanyId] = None) {

  def getMainCompanyMediaOpt = companyMedia.find(_.purpose == Some("main"))

  def getMainCompanyMediaUrl(imageSize: String) = getMainCompanyMediaOpt.map(_.url).asMediaUrl(imageSize, CompanyDefaultUrlType)

}
