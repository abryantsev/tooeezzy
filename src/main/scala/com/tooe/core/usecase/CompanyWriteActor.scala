package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.api.service._
import com.tooe.core.usecase.admin_user.AdminUserDataActor
import akka.pattern._
import com.tooe.core.domain._
import com.tooe.core.usecase.company.{PreModerationCompanyDataActor, CompanyDataActor}
import com.tooe.core.db.mongo.domain.PaymentLimit
import com.tooe.core.usecase.company.PreModerationCompanyDataActor.SaveCompany
import com.tooe.core.domain.CompanyId
import com.tooe.core.domain.AdminUserId
import com.tooe.core.db.mongo.domain.Phone
import com.tooe.core.db.mongo.domain.AdminUser
import com.tooe.core.db.mongo.domain.CompanyInformation
import com.tooe.core.domain.PreModerationCompanyId
import com.tooe.core.db.mongo.domain.CompanyManagement
import scala.Some
import com.tooe.core.usecase.admin_user.AdminUserDataActor.SaveAdminUser
import com.tooe.core.db.mongo.domain.CompanyPayment
import com.tooe.api.service.CompanyExportedRequest
import com.tooe.api.service.MainUser
import com.tooe.core.db.mongo.domain.CompanyContract
import com.tooe.core.db.mongo.domain.CompanyMedia
import com.tooe.core.db.mongo.domain.Company
import com.tooe.api.service.CompanyChangeRequest
import com.tooe.core.db.mongo.domain.CompanyContact
import com.tooe.core.db.mongo.domain.PreModerationCompany
import com.tooe.api.service.CompanyCreateRequest

object CompanyWriteActor {
  final val Id = Actors.CompanyWriteActor

  case class CompaniesExported(request: CompanyExportedRequest)

}


class CompanyWriteActor extends AppActor {

  import scala.concurrent.ExecutionContext.Implicits.global
  import CompanyWriteActor._

  lazy val companyDataActor = lookup(CompanyDataActor.Id)
  lazy val adminUserWriteActor = lookup(AdminUserDataActor.Id)

  def receive = {

    case CompaniesExported(request) =>
      companyDataActor ! CompanyDataActor.CompaniesExported(request)
      sender ! SuccessfulResponse

  }

}

