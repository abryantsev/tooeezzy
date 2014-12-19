package com.tooe.core.payment.platron

import org.specs2.mutable.SpecificationWithJUnit
import com.tooe.core.payment.plantron.PaymentSystemResponseParser

class PaymentSystemsResponseParserSpec extends SpecificationWithJUnit {

  "PaymentSystemsResponseParser" should {
    val fixture = new Fixture {}
    import PaymentSystemResponseParser._
    "parse xml response" >> {
      val paymentSystemResponse = parse(fixture.sampleResponse)

      paymentSystemResponse must not (beNull)
      paymentSystemResponse.systems must not (beEmpty)

      val cash = paymentSystemResponse.systems.find(_.name == "ALFACLICK").orNull
      cash must not (beNull)
      cash.description === "Наличные в кассах и платежных терминалах"
      cash.media.imageUrl must containing("ALFACLICK")
      cash.scenario === "offline"
      cash.amount === BigDecimal("110.00")
      cash.currencyCode === "RUR"
      cash.required === None

      cash.subSystems must not (beEmpty)
      val euroSet = cash.subSystems.get.find(_.name == "EUROSET").orNull
      euroSet must not (beNull)
      euroSet.description === "Салоны связи Евросеть"

      val moneyMailRu = paymentSystemResponse.systems.find(_.name == "MONEYMAILRU").orNull
      moneyMailRu.required === Some(Seq("pg_user_email", "pg_user_email2"))
      moneyMailRu.subSystems === None
    }
  }
}

trait Fixture {

  val sampleResponse = """
    <?xml version="1.0" encoding="UTF-8"?>
      <response>
        <pg_salt>14d214d0-bcb7-426e-b60d-e0d2a74c11bf</pg_salt>
        <pg_status>ok</pg_status>
        <pg_payment_system>
          <pg_name>ALFACLICK</pg_name>
          <pg_description>Наличные в кассах и платежных терминалах</pg_description>
          <pg_payment_scenario>offline</pg_payment_scenario>
          <pg_amount_to_pay>110.00</pg_amount_to_pay>
          <pg_amount_to_pay_currency>руб.</pg_amount_to_pay_currency>
          <pg_sub_payment_systems>
            <pg_sub_payment_system>
              <pg_sub_name>EUROSET</pg_sub_name>
              <pg_sub_description>Салоны связи Евросеть</pg_sub_description>
            </pg_sub_payment_system>
            <pg_sub_payment_system>
              <pg_sub_name>OSMP</pg_sub_name>
              <pg_sub_description>Терминалы QIWI / QIWI VISA WALLET</pg_sub_description>
            </pg_sub_payment_system>
            <pg_sub_payment_system>
              <pg_sub_name>CONTACT</pg_sub_name>
              <pg_sub_description>Система приёма платежей CONTACT</pg_sub_description>
            </pg_sub_payment_system>
            <pg_sub_payment_system>
              <pg_sub_name>EUROPLAT</pg_sub_name>
              <pg_sub_description>Салоны связи Евросеть (Европлат)</pg_sub_description>
            </pg_sub_payment_system>
            <pg_sub_payment_system>
              <pg_sub_name>MASTERBANK</pg_sub_name>
              <pg_sub_description>Оплата через банкоматы Мастер-Банка</pg_sub_description>
            </pg_sub_payment_system>
            <pg_sub_payment_system>
              <pg_sub_name>ELECSNET</pg_sub_name>
              <pg_sub_description>Терминалы Элекснет</pg_sub_description>
            </pg_sub_payment_system>
            <pg_sub_payment_system>
              <pg_sub_name>SVYAZNOY</pg_sub_name>
              <pg_sub_description>Оплата через салоны сотовой связи "Связной"</pg_sub_description>
            </pg_sub_payment_system>
          </pg_sub_payment_systems>
        </pg_payment_system>
        <pg_payment_system>
          <pg_name>ALFACLICK</pg_name>
          <pg_description>Интернет-банк "Альфа-Клик"</pg_description>
          <pg_payment_scenario>online</pg_payment_scenario>
          <pg_amount_to_pay>101.99</pg_amount_to_pay>
          <pg_amount_to_pay_currency>руб.</pg_amount_to_pay_currency>
          <pg_required>pg_alfaclick_client_id</pg_required>
        </pg_payment_system>
        <pg_payment_system>
          <pg_name>YANDEXMONEY</pg_name>
          <pg_description>Яндекс.Деньги</pg_description>
          <pg_payment_scenario>online</pg_payment_scenario>
          <pg_amount_to_pay>107.09</pg_amount_to_pay>
          <pg_amount_to_pay_currency>руб.</pg_amount_to_pay_currency>
        </pg_payment_system>
        <pg_payment_system>
          <pg_name>WEBMONEYRBANK</pg_name>
          <pg_description>Webmoney</pg_description>
          <pg_payment_scenario>online</pg_payment_scenario>
          <pg_amount_to_pay>105.46</pg_amount_to_pay>
          <pg_amount_to_pay_currency>WMR</pg_amount_to_pay_currency>
        </pg_payment_system>
        <pg_payment_system>
          <pg_name>MONEYMAILRU</pg_name>
          <pg_description>Деньги@Mail.ru</pg_description>
          <pg_payment_scenario>online</pg_payment_scenario>
          <pg_amount_to_pay>105.46</pg_amount_to_pay>
          <pg_amount_to_pay_currency>руб.</pg_amount_to_pay_currency>
          <pg_required>pg_user_email</pg_required>
          <pg_required>pg_user_email2</pg_required>
        </pg_payment_system>
        <pg_payment_system>
          <pg_name>MOBILEPHONE</pg_name>
          <pg_description>Оплата с мобильного телефона</pg_description>
          <pg_payment_scenario>offline</pg_payment_scenario>
          <pg_amount_to_pay>107.60</pg_amount_to_pay>
          <pg_amount_to_pay_currency>руб.</pg_amount_to_pay_currency>
          <pg_sub_payment_systems>
            <pg_sub_payment_system>
              <pg_sub_name>INPLATMEGAFON</pg_sub_name>
              <pg_sub_description>Оплата с телефонов Megafon через платежную систему InPlat</pg_sub_description>
            </pg_sub_payment_system>
            <pg_sub_payment_system>
              <pg_sub_name>INPLATMTS</pg_sub_name>
              <pg_sub_description>Оплата с телефонов МТС через платежную систему InPlat</pg_sub_description>
            </pg_sub_payment_system>
          </pg_sub_payment_systems>
        </pg_payment_system>
        <pg_payment_system>
          <pg_name>PSB</pg_name>
          <pg_description>Оплата через интернет-банк Промсвязьбанка</pg_description>
          <pg_payment_scenario>online</pg_payment_scenario>
          <pg_amount_to_pay>101.99</pg_amount_to_pay>
          <pg_amount_to_pay_currency>руб.</pg_amount_to_pay_currency>
        </pg_payment_system>
        <pg_sig>960119ab0c9742b7ddb7149a42b3c0b8</pg_sig>
      </response>
  """
}