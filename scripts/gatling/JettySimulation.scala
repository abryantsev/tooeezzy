package tooeezzy 
import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import akka.util.duration._
import bootstrap._
import assertions._

class JettySimulation extends Simulation {

  val host = "localhost"
  val port = "8443"

	val httpConf = httpConfig
			.baseURL("https://" + host + ":" + port)
			.acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
			.acceptEncodingHeader("gzip, deflate")
			.acceptLanguageHeader("ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3")
			.userAgentHeader("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:16.0) Gecko/20100101 Firefox/16.0")


	val headers_1 = Map(
			"Cache-Control" -> """max-age=0""",
			"Connection" -> """keep-alive"""
	)

	val scn_main = scenario("Main Load")
		.exec(http("request_1")
					.get("/mongo/testget")
					.headers(headers_1)
			)

  val scn_peak = scenario("Peak Load")
    .exec(http("request_1")
          .get("/mongo/testget")
          .headers(headers_1)
      )

  setUp(scn_main.users(5000).ramp(150).protocolConfig(httpConf),
        scn_peak.users(40000).ramp(20).delay(30).protocolConfig(httpConf))

  assertThat(global.responseTime.max.lessThan(100))
}