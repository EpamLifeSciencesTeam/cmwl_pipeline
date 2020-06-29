package cromwell.pipeline.datastorage.dao.repository.auth

import java.time.Instant

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse, StatusCodes }
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ RejectionHandler, Route, ValidationRejection }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.utils.auth.SecurityDirective._
import cromwell.pipeline.datastorage.utils.auth.{ AccessTokenContent, AuthContent, SecurityDirective }
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.utils.{ AuthConfig, ExpirationTimeInSeconds, MissingAccessTokenRejection }
import org.scalatest.{ Matchers, WordSpec }
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{ Jwt, JwtAlgorithm, JwtClaim }
import play.api.libs.json.Json

class SecurityDirectiveTest extends WordSpec with Matchers with ScalatestRouteTest {

  private val authConfig = AuthConfig(
    secretKey = "secretKey",
    hmacAlgorithm = JwtAlgorithm.fromString(algo = "HS256").asInstanceOf[JwtHmacAlgorithm],
    expirationTimeInSeconds = ExpirationTimeInSeconds(accessToken = 300, refreshToken = 900, userSession = 3600)
  )
  private val securityDirective = new SecurityDirective(authConfig)
  private val publicPath = "publicContent"
  private val securedPath = "securedContent"
  private val publicContent = "Public content is available."
  private val securedContent = "Secret content is available."

  implicit val rejectionHandler = RejectionHandler
    .newBuilder()
    .handle {
      case ValidationRejection(msg, _)      => complete(StatusCodes.BadRequest, msg)
      case MissingAccessTokenRejection(msg) => complete(StatusCodes.Unauthorized, msg)
    }
    .result()
    .withFallback(RejectionHandler.default)

  val testRoute = Route.seal(
    concat(
      path(publicPath) {
        get {
          complete(publicContent)
        }
      },
      securityDirective.authenticated { _ =>
        path(securedPath) {
          get {
            complete(securedContent)
          }
        }
      }
    )
  )

  "SecurityDirective" should {

    "not block public content" in {
      Get(s"/$publicPath") ~> testRoute ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe publicContent
      }
    }

    "not block secured content with active access token" in {
      val accessToken = getAccessToken(lifetimeInSeconds = 3600)
      val header = RawHeader(AuthorizationHeader, accessToken)
      Get(s"/$securedPath").withHeaders(header) ~> testRoute ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe securedContent
      }
    }

    "block secured content without access token" in {
      Get(s"/$securedPath") ~> testRoute ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    "block secured content with incorrect access token format" in {
      val header = RawHeader(AuthorizationHeader, "incorrectAccessTokenFormat")
      Get(s"/$securedPath").withHeaders(header) ~> testRoute ~> check {
        status shouldBe StatusCodes.Unauthorized
        responseAs[String] shouldBe UnauthorizedMessages.InvalidToken
      }
    }

    "block secured content with expired access token" in {
      val accessToken = getAccessToken(lifetimeInSeconds = 0)
      val header = RawHeader(AuthorizationHeader, accessToken)
      Get(s"/$securedPath").withHeaders(header) ~> testRoute ~> check {
        status shouldBe StatusCodes.Unauthorized
        responseAs[String] shouldBe UnauthorizedMessages.InvalidToken
      }
    }

  }

  private def getAccessToken(lifetimeInSeconds: Long): String = {
    val currentTimestamp = Instant.now.getEpochSecond
    val accessTokenContent: AuthContent = AccessTokenContent(UserId.random)
    val claims = JwtClaim(
      content = Json.stringify(Json.toJson(accessTokenContent)),
      expiration = Some(currentTimestamp + lifetimeInSeconds),
      issuedAt = Some(currentTimestamp)
    )

    Jwt.encode(claims, authConfig.secretKey, authConfig.hmacAlgorithm)
  }
}
