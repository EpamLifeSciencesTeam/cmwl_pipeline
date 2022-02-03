package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import cromwell.pipeline.datastorage.dto.EmptyPayload
import cromwell.pipeline.service.SuccessResponseBody
import cromwell.pipeline.utils.{ AkkaTestSources, DummyObject }
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json

class AkkaHttpClientTest extends AsyncWordSpec with Matchers with MockitoSugar with AkkaTestSources {

  val wireMockServer = new WireMockServer(wireMockConfig().dynamicPort())
  val client = new AkkaHttpClient()

  val dummyObject: DummyObject = DummyObject(12345, "something as object content")
  val stringifiedRespBody = """{"someId":12345,"someString":"something as object content"}"""
  val successRespBody = SuccessResponseBody(dummyObject)
  val applicationJson = "application/json"

  val params = Map("id" -> "1")
  val headers = Map("Language" -> "eng")
  val payload = dummyObject
  val OK = StatusCodes.OK.intValue

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
        val response = aResponse()
          .withBody(Json.stringify(Json.toJson(dummyObject)))
          .withStatus(OK)
          .withHeader("Content-Type", applicationJson)
        wireMockServer.stubFor(get(urlEqualTo("/get?id=1")).willReturn(response))
        val get_url = s"${wireMockServer.baseUrl()}/get"

        client.get[DummyObject](get_url, params, headers).flatMap(_.status shouldBe OK)
      }

      "return response with body" taggedAs Controller in {
        val response = aResponse()
          .withBody(Json.stringify(Json.toJson(dummyObject)))
          .withStatus(OK)
          .withHeader("Content-Type", applicationJson)
        wireMockServer.stubFor(get(urlEqualTo("/get?id=1")).willReturn(response))
        val get_url = s"${wireMockServer.baseUrl()}/get"

        client.get[DummyObject](get_url, params, headers).flatMap(_.body shouldBe successRespBody)
      }

      "return response with headers" taggedAs Controller in {
        val response = aResponse()
          .withBody(Json.stringify(Json.toJson(dummyObject)))
          .withStatus(OK)
          .withHeader("TestKey", "TestValue")
          .withHeader("Content-Type", applicationJson)
        wireMockServer.stubFor(get(urlEqualTo("/get?id=1")).willReturn(response))
        val get_url = s"${wireMockServer.baseUrl()}/get"

        client
          .get[DummyObject](get_url, params, headers)
          .flatMap(_.headers.getOrElse("TestKey", List("failed")) shouldBe List("TestValue"))
      }
    }

    "delete" should {
      "return ok response status" taggedAs Controller in {
        val response = aResponse().withStatus(OK)

        wireMockServer.stubFor(
          delete(urlEqualTo("/delete?id=1")).willReturn(response)
        )

        val deleteUrl = s"${wireMockServer.baseUrl()}/delete"
        client.delete[EmptyPayload, DummyObject](deleteUrl, params, headers, payload).flatMap(_.status shouldBe OK)
      }
    }

    "post" should {
      "return OK response status" taggedAs Controller in {
        val response = aResponse()
          .withBody(Json.stringify(Json.toJson(dummyObject)))
          .withStatus(OK)
          .withHeader("Content-Type", applicationJson)
        wireMockServer.stubFor(
          post(urlEqualTo("/post?id=1")).withRequestBody(equalTo(stringifiedRespBody)).willReturn(response)
        )
        val post_url = s"${wireMockServer.baseUrl()}/post"

        client.post[DummyObject, DummyObject](post_url, params, headers, payload).flatMap(_.status shouldBe OK)
      }

      "return response with body" taggedAs Controller in {
        val response = aResponse()
          .withBody(Json.stringify(Json.toJson(dummyObject)))
          .withStatus(OK)
          .withHeader("Content-Type", applicationJson)
        wireMockServer.stubFor(
          post(urlEqualTo("/post?id=1")).withRequestBody(equalTo(stringifiedRespBody)).willReturn(response)
        )
        val post_url = s"${wireMockServer.baseUrl()}/post"

        client
          .post[DummyObject, DummyObject](post_url, params, headers, payload)
          .flatMap(_.body shouldBe successRespBody)
      }

      "return response with headers" taggedAs Controller in {
        val response = aResponse()
          .withBody(Json.stringify(Json.toJson(dummyObject)))
          .withStatus(OK)
          .withHeader("TestKey", "TestValue")
          .withHeader("Content-Type", applicationJson)
        wireMockServer.stubFor(
          post(urlEqualTo("/post?id=1")).withRequestBody(equalTo(stringifiedRespBody)).willReturn(response)
        )
        val post_url = s"${wireMockServer.baseUrl()}/post"

        client
          .post[DummyObject, DummyObject](post_url, params, headers, payload)
          .flatMap(_.headers.getOrElse("TestKey", List("failed")) shouldBe List("TestValue"))
      }
    }

    "put" should {
      "return OK response status" taggedAs Controller in {
        val response = aResponse()
          .withBody(Json.stringify(Json.toJson(dummyObject)))
          .withStatus(OK)
          .withHeader("Content-Type", applicationJson)
        wireMockServer.stubFor(
          put(urlEqualTo("/put?id=1")).withRequestBody(equalTo(stringifiedRespBody)).willReturn(response)
        )
        val put_url = s"${wireMockServer.baseUrl()}/put"

        client.put[DummyObject, DummyObject](put_url, params, headers, payload).flatMap(_.status shouldBe OK)
      }

      "return response with body" taggedAs Controller in {
        val response = aResponse()
          .withBody(Json.stringify(Json.toJson(dummyObject)))
          .withStatus(OK)
          .withHeader("Content-Type", applicationJson)
        wireMockServer.stubFor(
          put(urlEqualTo("/put?id=1")).withRequestBody(equalTo(stringifiedRespBody)).willReturn(response)
        )
        val put_url = s"${wireMockServer.baseUrl()}/put"

        client.put[DummyObject, DummyObject](put_url, params, headers, payload).flatMap(_.body shouldBe successRespBody)
      }

      "return response with headers" taggedAs Controller in {
        val response = aResponse()
          .withBody(Json.stringify(Json.toJson(dummyObject)))
          .withStatus(OK)
          .withHeader("TestKey", "TestValue")
          .withHeader("Content-Type", applicationJson)
        wireMockServer.stubFor(
          put(urlEqualTo("/put?id=1")).withRequestBody(equalTo(stringifiedRespBody)).willReturn(response)
        )
        val put_url = s"${wireMockServer.baseUrl()}/put"

        client
          .put[DummyObject, DummyObject](put_url, params, headers, payload)
          .flatMap(_.headers.getOrElse("TestKey", List("failed")) shouldBe List("TestValue"))
      }
    }
  }
}
