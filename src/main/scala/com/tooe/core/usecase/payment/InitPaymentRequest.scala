package com.tooe.core.usecase.payment

import com.tooe.api.service.RouteContext
import com.tooe.core.domain.UserId

case class InitPaymentRequest(request: PaymentRequest, routeContext: RouteContext, userId: UserId)