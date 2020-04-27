package cromwell.pipeline.controller

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.ExecutionContext

class AkkaHttpClientTest extends AsyncWordSpec with Matchers with MockitoSugar with BeforeAndAfterAll {
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = actorSystem.dispatcher

  val wireMockServer = new WireMockServer(wireMockConfig().dynamicPort())
  val client = new AkkaHttpClient()

  override def beforeAll: Unit = {
    super.beforeAll()
    wireMockServer.start()
  }

  override def afterAll: Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

  "AkkaHttpControllerTest" when {

    "get" should {
      "return OK response status" taggedAs Controller in {
        val response = aResponse().withStatus(200)
        wireMockServer.stubFor(get(urlEqualTo("/get?id=1")).willReturn(response))

        val get_url = s"${wireMockServer.baseUrl()}/get"
        val params = Map("id" -> "1")
        val headers = Map("Language" -> "eng")

        client.get(get_url, params, headers).flatMap(_.status shouldBe StatusCodes.OK.intValue)
      }

      "return response with body" taggedAs Controller in {
        val response = aResponse().withBody("Value").withStatus(200)
        wireMockServer.stubFor(get(urlEqualTo("/get?id=1")).willReturn(response))

        val get_url = s"${wireMockServer.baseUrl()}/get"
        val params = Map("id" -> "1")
        val headers = Map("Language" -> "eng")

        client.get(get_url, params, headers).flatMap(_.body shouldBe "Value")
      }

      "return response with headers" taggedAs Controller in {
        val response = aResponse().withHeader("TestKey", "TestValue").withStatus(200)
        wireMockServer.stubFor(get(urlEqualTo("/get?id=1")).willReturn(response))

        val get_url = s"${wireMockServer.baseUrl()}/get"
        val params = Map("id" -> "1")
        val headers = Map("Language" -> "eng")

        client.get(get_url, params, headers).flatMap(_.headers.getOrElse("TestKey", List("failed")) shouldBe List("TestValue"))
      }
    }

    "post" should {
      "return OK response status" taggedAs Controller in {
        val response = aResponse().withStatus(200)
        wireMockServer.stubFor(
          post(urlEqualTo("/post?id=1")).withRequestBody(equalTo("test payload")).willReturn(response)
        )

        val post_url = s"${wireMockServer.baseUrl()}/post"
        val params = Map("id" -> "1")
        val headers = Map("Language" -> "eng")
        val payload = "test payload"

        client.post(post_url, params, headers, payload).flatMap(_.status shouldBe StatusCodes.OK.intValue)
      }

      "return response with body" taggedAs Controller in {
        val response = aResponse().withBody("Value").withStatus(200)
        wireMockServer.stubFor(
          post(urlEqualTo("/post?id=1")).withRequestBody(equalTo("test payload")).willReturn(response)
        )

        val post_url = s"${wireMockServer.baseUrl()}/post"
        val params = Map("id" -> "1")
        val headers = Map("Language" -> "eng")
        val payload = "test payload"

        client.post(post_url, params, headers, payload).flatMap(_.body shouldBe "Value")
      }

      "return response with headers" taggedAs Controller in {
        val response = aResponse().withHeader("TestKey", "TestValue").withStatus(200)
        wireMockServer.stubFor(
          post(urlEqualTo("/post?id=1")).withRequestBody(equalTo("test payload")).willReturn(response)
        )

        val post_url = s"${wireMockServer.baseUrl()}/post"
        val params = Map("id" -> "1")
        val headers = Map("Language" -> "eng")
        val payload = "test payload"

        client.post(post_url, params, headers, payload).flatMap(_.headers.getOrElse("TestKey", List("failed")) shouldBe List("TestValue"))
      }
    }

    "put" should {
      "return OK response status" taggedAs Controller in {
        val response = aResponse().withStatus(200)
        wireMockServer.stubFor(
          put(urlEqualTo("/put?id=1")).withRequestBody(equalTo("test payload")).willReturn(response)
        )

        val put_url = s"${wireMockServer.baseUrl()}/put"
        val params = Map("id" -> "1")
        val headers = Map("Language" -> "eng")
        val payload = "test payload"

        client.put(put_url, params, headers, payload).flatMap(_.status shouldBe StatusCodes.OK.intValue)
      }

      "return response with body" taggedAs Controller in {
        val response = aResponse().withBody("Value").withStatus(200)
        wireMockServer.stubFor(
          put(urlEqualTo("/put?id=1")).withRequestBody(equalTo("test payload")).willReturn(response)
        )

        val put_url = s"${wireMockServer.baseUrl()}/put"
        val params = Map("id" -> "1")
        val headers = Map("Language" -> "eng")
        val payload = "test payload"

        client.put(put_url, params, headers, payload).flatMap(_.body shouldBe "Value")
      }

      "return response with headers" taggedAs Controller in {
        val response = aResponse().withHeader("TestKey", "TestValue").withStatus(200)
        wireMockServer.stubFor(
          put(urlEqualTo("/put?id=1")).withRequestBody(equalTo("test payload")).willReturn(response)
        )

        val put_url = s"${wireMockServer.baseUrl()}/put"
        val params = Map("id" -> "1")
        val headers = Map("Language" -> "eng")
        val payload = "test payload"

        client.put(put_url, params, headers, payload).flatMap(_.headers.getOrElse("TestKey", List("failed")) shouldBe List("TestValue"))
      }
    }
  }
}
