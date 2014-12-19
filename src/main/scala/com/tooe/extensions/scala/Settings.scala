package com.tooe.extensions.scala

import akka.actor._

import com.typesafe.config.Config
import java.util.concurrent.TimeUnit
import com.tooe.core.usecase.payment
import akka.util.Timeout
import scala.util.Try
import scala.concurrent.duration._
import scala.collection.JavaConverters._
import com.tooe.core.domain.UrlType
import com.tooe.core.util.Lang
import com.tooe.core.usecase.payment.ResponseType

class SettingsImpl(config: Config) extends Extension {

  def this(context: ActorContext) {
    this(context.system.settings.config)
  }

  import config._

  println("tooe >> ... init SettingsImpl ...")

  // === general ===
//  	val DEFAULT_TIMEOUT = getInt("akka.default-timeout")
  	val DEFAULT_TIMEOUT: Timeout = Timeout(getInt("akka.default-timeout"))
  	val DEFAULT_ACTOR_TIMEOUT: Timeout = Timeout(getInt("akka.timeouts.default-timeout"))

  // === graph ===
//  val TITAN_STORAGE_BACKEND = getString("akka.tooe.titan.storage-backend")
//  val TITAN_STORAGE_HOSTNAME = getString("akka.tooe.titan.storage-hostname")
//  val TITAN_REXTER_HOSTNAME = getString("akka.tooe.titan.rexter-hostname")
//  val TITAN_AUTOTYPE = getString("akka.tooe.titan.autotype")

  // === riak ===
//  val RIAK_NODE1 = getString("akka.tooe.riak.node1")
//  val RIAK_TOTAL_MAX_CONN = getInt("akka.tooe.riak.total-max-conn")
//  val RIAK_IDLE_CONN_TTL_MILLS = getInt("akka.tooe.riak.idle-conn-ttl-mills")
//  val RIAK_REQ_TIMEOUT_MILLS = getInt("akka.tooe.riak.req-timeout-mills")

  // === SSL ===
  val KeyStorePath = getString("ssl.key.store.path")
  val KeyStorePassword = getString("ssl.key.store.password")
  //val KeyStorePath = getClass.getClassLoader.getResource("jettyKeyStore").getPath
  //val KeyStorePassword = "tooejetty"

  // === spray can ===
  val SprayCanServerHttpStart = getString("spray.can.server.http.start")
  val SprayCanServerHttpHost = getString("spray.can.server.http.host")
  val SprayCanServerHttpPort = getInt("spray.can.server.http.port")
  val SprayCanServerHttpsStart = getString("spray.can.server.https.start")
  val SprayCanServerHttpsHost = getString("spray.can.server.https.host")

  // === jetty ===
  val JettyServerContextPath = getString("jetty.server.context-path")
  val JettyServerConnectorServletPath = getString("jetty.server.connector-servlet-path")
  val JettyServerWarPath = getString("jetty.server.war-path")

  val JettyServerHttpStart = getString("jetty.server.http.start")
  val JettyServerHttpHost = getString("jetty.server.http.host")
  val JettyServerHttpPort = getInt("jetty.server.http.port")
  val JettyServerHttpsStart = getString("jetty.server.https.start")
  val JettyServerHttpsHost = getString("jetty.server.https.host")
  val JettyServerHttpsPort = getInt("jetty.server.https.port")

  // === akka tooe ===
  val CircuitBreakerTimeout: Duration =
    Duration(getMilliseconds("akka.tooe.circuit-breaker.timeout"),
      TimeUnit.MILLISECONDS)

  val tooeProfile = getString("akka.tooe.deployer.profile")
  val initGraphService = getBoolean("akka.tooe.deployer.initgraphservice")
  val tooeClustering = getBoolean("akka.tooe.deployer.clustering")
  val withIntegrationService = getBoolean("akka.tooe.deployer.withIntegrationService")
  // === ==== ===

  def startSprayCanHttp() = "on".equals(SprayCanServerHttpStart)

  def startSprayCanHttps() = "on".equals(SprayCanServerHttpsStart)

  def startJettyHttp() = "on".equals(JettyServerHttpStart)

  def startJettyHttps() = "on".equals(JettyServerHttpsStart)

  object Spring {
    val ApplicationContextPath: String = getString("spring.applicationContextPath")
  }

  object Payment {
    val PaymentExpiresInMin = getInt("payment.payment-expires-in-min")
  }
  
  object Platron {
    val Prefix = "payment.platron"
    
    implicit def urlHelper(url: String) = new {
      def substitute(orderId: String, lang: Lang) =
        url.replaceAllLiterally("$ORDER", orderId).replaceAllLiterally("$LANG", lang.id)

      def responseType(responseType: ResponseType) = url + "?responsetype=" + responseType.id
    }

    val MerchantId: String = getString(Prefix+".merchantId")
    val SecretKey: String = getString(Prefix+".secretKey")
    val TestingMode: Boolean = getBoolean(Prefix+".testingMode")
    val RequestMethod: String = getString(Prefix+".requestMethod")
    def checkUrl(orderId: String, lang: Lang): String = getString(Prefix+".checkUrl").substitute(orderId, lang)
    def resultUrl(orderId: String, lang: Lang): String = getString(Prefix+".resultUrl").substitute(orderId, lang)

    object Redirect {
      sealed trait Result {
        protected def result: String

        def url(responseType: payment.ResponseType, orderId: String, lang: Lang): String =
          getString(s"$Prefix.redirect.$result.url").substitute(orderId, lang).responseType(responseType)

        def method(responseType: payment.ResponseType, orderId: String, lang: Lang): String =
          getString(s"$Prefix.redirect.$result.method").substitute(orderId, lang)
      }
      object Success extends Result {
        protected def result: String = "success"
      }
      object Failure extends Result {
        protected def result: String = "failure"
      }
    }

    object PaymentSystem {
      val Host: String = getString(Prefix+".paymentSystem.host")
      val Script: String = getString(Prefix+".paymentSystem.script")
      val Timeout: Long = getLong(Prefix+".paymentSystem.timeout")
    }
  }

  object Smtp {
    val prefix = "smtp"

    val Host = getString(s"${prefix}.host")
    val DebugMode = getString(s"${prefix}.debugMode")
    val DefaultEncoding = getString(s"${prefix}.defaultEncoding")
  }  

  object Session {
    val TimestampExpiresInSec = getLong("session.timestamp-expires-in-sec")
  }

  object MediaServer {
    val prefix = "mediaserver"

    lazy val Host = getString(s"${prefix}.host")

    val DEFAULT_TIMEOUT = Timeout(getLong(s"$prefix.default-timeout"))

    def getImageSize(name: String)(view: String)(obj: String)(field: String):String = {
      config.getString(s"$name.$view.$obj.$field")
    }

    def mediaServerType(imageType: String) = {
      getString(s"${prefix}.${imageType}.mediaservertype")
    }

    def getUrlSuffix(imageType: String) = getString(s"${prefix}.upload-type.${imageType}")

  }

  object MigrationMediaServer {
    val prefix = "migrationmediaserver"

    val Host = getString(s"${prefix}.host")
  }

  object GeoSearch {

      object CheckinSearch {
        val prefix = "geo-search.checkins-search"

        val RadiusDefault = getInt(s"${prefix}.radius-meters")
      }
      object LocationSearch {
        val prefix = "geo-search.locations-search"

        val RadiusMax = getInt(s"${prefix}.radius.max")
        val FriendsCount = getInt(s"${prefix}.friends.count ")
        val ResultCount = getInt(s"${prefix}.result.count")

      }
    object LocationsForCheckinSearch {
        private val prefix = "geo-search.locations-for-checkin-search"

        val RadiusMax = getInt(s"${prefix}.radius.max")
      }

  }

  object Api {
    val prefix = "api"
    val coreRoutesEnable = getBoolean(s"${prefix}.core-routes-enable")
    val testCoreRoutesEnable = getBoolean(s"${prefix}.test-core-routes-enable")
    val testGraphRoutesEnable = getBoolean(s"${prefix}.test-graph-routes-enable")
    val migrationRoutesEnable = getBoolean(s"${prefix}.migration-routes-enable")
    val correctionRoutesEnable = getBoolean(s"${prefix}.correction-routes-enable")
  }

  object Security {
    private val prefix = "security"

    val WriteRequestsPerMinute = getInt(s"${prefix}.write-requests-per-minute")
    val TooeDSignHeaderName = getString(s"${prefix}.tooe-dsign-header-name")
    val TooeDSignPassword = getString(s"${prefix}.tooe-dsign-password")
  }

  object PhotoUpload {
    private val prefix = "photo-upload"

    val AggregateUploadedPhotoTime = getMilliseconds(s"${prefix}.aggregate-uploaded-photo-time-interval")
  }

  object ImagesDimensions {
    private val prefix = "images"
    def getStringByPath(path: String) = getString(s"${prefix}.${path}")
    object UserProfile {
      private val userProfile = "user_profile"
      object Short {
        private val short = "short"
        val MediaMain = getString(s"${prefix}.${userProfile}.${short}.media.main")
      }
      object Full {
        private val full = "full"
        val MediaDefault = getString(s"${prefix}.${userProfile}.${full}.media.default")
      }
    }
    object UserMain {
      private val userMainScreen = "user_mainscreen"
      object Full {
        private val full = "full"
        val MediaDefault = getString(s"${prefix}.${userMainScreen}.${full}.media.default")
      }
    }
  }

  object CDN {
    private val prefix = "cdn"

    val host = getString(s"${prefix}.host")
  }

  object StaticCDN {
    private val prefix = "cdn_static"

    val host = getString(s"${prefix}.host")
  }

  object S3 {
    private val prefix = "s3"

    val host = getString(s"${prefix}.host")
  }

  object HttpImages {
    val showHttpImages = Try(getBoolean("images.showhttpimages")).toOption.getOrElse(false)
  }

  object DefaultImages {
    private val prefix = "images.default_images_names"

    val location = getString(s"${prefix}.location.media")
    val company = getString(s"${prefix}.company.media")
    val product = getString(s"${prefix}.product.media")
    val promotion = getString(s"${prefix}.promotion.media")
    val photo = getString(s"${prefix}.photo.media")
    val femaleUser = getString(s"${prefix}.user.media.f")
    val maleUser = getString(s"${prefix}.user.media.m")
    val background = getString(s"${prefix}.user.media.bg")
  }

  object Job {
    private val prefix = "job"

    object UrlCheck {
      private val urlCheck = "url_check"
      private val jobPrefix = s"$prefix.$urlCheck"

      val enable = Try(getBoolean(s"$jobPrefix.enable")).toOption.getOrElse(false)
      val delay = Try(getInt(s"$jobPrefix.delay")).toOption.map(_.millis).getOrElse(5 minutes)
      val interval = Try(getInt(s"$jobPrefix.interval")).toOption.map(_.millis).getOrElse(10 minutes)
      val size = Try(getInt(s"$jobPrefix.size")).toOption.getOrElse(100)
      val showLog = Try(getBoolean(s"$jobPrefix.showlog")).toOption.getOrElse(false)
      val readOffset = Try(getInt(s"$jobPrefix.readoffset")).toOption.map(_.millis).getOrElse(1800 seconds)
      val types = Try(getStringList(s"$jobPrefix.types")).toOption.map(l => l.asScala.map(UrlType(_))).getOrElse(Nil)

      def imageSizes(entityType: String): Seq[String] =
        Try(getStringList(s"${prefix}.${urlCheck}.image_sizes.${entityType}").asScala).toOption.getOrElse(Nil)

      val TIMEOUT: Timeout = Timeout(getInt("job.url_check.timeout.httpupload"))

    }

    object OverduePaymentCancellation {
      val enable = getBoolean("job.overdue-payment-cancellation.enable")
      val interval = FiniteDuration(getMilliseconds("job.overdue-payment-cancellation.interval"), TimeUnit.MILLISECONDS)
      val initialDelay = FiniteDuration(getMilliseconds("job.overdue-payment-cancellation.initialDelay"), TimeUnit.MILLISECONDS)
    }

    object ExpiredPresentsMarker {
      val enable = getBoolean(s"$prefix.expired_presents_marker.enable")
      val interval = FiniteDuration(getMilliseconds(s"$prefix.expired_presents_marker.interval"), TimeUnit.MILLISECONDS)
      val initialDelay = FiniteDuration(getMilliseconds(s"$prefix.expired_presents_marker.initialDelay"), TimeUnit.MILLISECONDS)
    }
  }

}

object Settings extends ExtensionId[SettingsImpl] with ExtensionIdProvider {

  println("tooe >> ... init Settings ...")
  
  override def lookup = Settings

  override def createExtension(system: ExtendedActorSystem) =
    new SettingsImpl(system.settings.config)
}