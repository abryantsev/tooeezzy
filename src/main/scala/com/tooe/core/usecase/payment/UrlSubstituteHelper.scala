package com.tooe.core.usecase.payment

import com.tooe.core.util.Lang

object UrlSubstituteHelper {

  val LangPlaceholder = "$LANG"
  val OrderIdPlaceholder = "$ORDER"

  def apply(url: String, paymentUuid: String, lang: Lang, responseType: ResponseType): String = url
    .replaceAllLiterally(OrderIdPlaceholder, paymentUuid)
    .replaceAllLiterally(LangPlaceholder, lang.id) + "?responsetype="+responseType.id
}