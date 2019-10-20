package cromwell.pipeline.controller

import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.controller.AuthController._
import cromwell.pipeline.datastorage.dto.auth.{AuthResponse, SignInRequest, SignUpRequest}
import cromwell.pipeline.service.AuthService
import cromwell.pipeline.utils.validator.DomainValidation
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Assertion, Matchers, WordSpec}
import play.api.libs.json.Json

import scala.concurrent.Future

class AuthControllerTest extends WordSpec with Matchers with MockFactory with ScalatestRouteTest {

  private val authService = stub[AuthService]
  private val authController = new AuthController(authService)
  private val accessToken = "access-token"
  private val refreshToken = "refresh-token"
  private val accessTokenExpiration = 300

  "AuthController" when {

    "signIn" should {

      "return token headers if user exists" in {
        val signInRequest = SignInRequest("email", "password")
        val authResponse = AuthResponse(accessToken, refreshToken, accessTokenExpiration)
        val httpEntity = HttpEntity(`application/json`, Json.stringify(Json.toJson(signInRequest)))
        authService.signIn _ when signInRequest returns Future(Option(authResponse))

        Post("/auth/signIn", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.OK
          checkAuthTokens
        }
      }

      "return Unauthorized status if user doesn't exist" in {
        val signInRequest = SignInRequest("email", "password")
        val httpEntity = HttpEntity(`application/json`, Json.stringify(Json.toJson(signInRequest)))
        authService.signIn _ when signInRequest returns Future(None)

        Post("/auth/signIn", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }
    }

    "signUp" should {

      "return token headers if user was successfully registered" in {
        val signUpRequest = SignUpRequest("JohnDoe@cromwell.com", "Password213", "FirstName", "LastName")
        val authResponse = AuthResponse(accessToken, refreshToken, accessTokenExpiration)
        val httpEntity = HttpEntity(`application/json`, Json.stringify(Json.toJson(signUpRequest)))
        authService.signUp _ when signUpRequest returns Future(Some(authResponse))

        Post("/auth/signUp", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.OK
          checkAuthTokens
        }
      }

      "return BadRequest with fields validation errors" in {
        val signUpRequest = SignUpRequest("email", "password", "First-name", "Last-name")
        val authResponse = AuthResponse(accessToken, refreshToken, accessTokenExpiration)
        val httpEntity = HttpEntity(`application/json`, Json.stringify(Json.toJson(signUpRequest)))
        authService.signUp _ when signUpRequest returns Future(Some(authResponse))

        Post("/auth/signUp", httpEntity) ~> authController.route ~> check {
          val errors = Json.parse(responseAs[String]).as[List[Map[String, String]]]
          val errorCodes = errors.map(_("errorCode")).toSet

          status shouldBe StatusCodes.BadRequest
          DomainValidation.allErrorCodes.forall(errorCodes.contains) shouldBe true
        }
      }

      "return BadRequest status if user registration was failed" in {
        val signUpRequest = SignUpRequest("email", "password", "First-name", "Last-name")
        val httpEntity = HttpEntity(`application/json`, Json.stringify(Json.toJson(signUpRequest)))
        authService.signUp _ when signUpRequest returns Future(throw new RuntimeException("Something wrong."))

        Post("/auth/signUp", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
    }

    "refresh" should {

      "return updated token headers if refresh token was valid and active" in {
        val authResponse = AuthResponse(accessToken, refreshToken, accessTokenExpiration)
        authService.refreshTokens _ when refreshToken returns Some(authResponse)

        Get(s"/auth/refresh?refreshToken=$refreshToken") ~> authController.route ~> check {
          status shouldBe StatusCodes.OK
          checkAuthTokens
        }
      }

      "return BadRequest status if something was wrong with refresh token" in {
        authService.refreshTokens _ when refreshToken returns None

        Get(s"/auth/refresh?refreshToken=$refreshToken") ~> authController.route ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
    }

  }

  private def checkAuthTokens: Assertion = {
    getOptHeaderValue(AccessTokenHeader) should contain(accessToken)
    getOptHeaderValue(RefreshTokenHeader) should contain(refreshToken)
    getOptHeaderValue(AccessTokenExpirationHeader) should contain(accessTokenExpiration.toString)
  }

  private def getOptHeaderValue(name: String): Option[String] = header(name).map(_.value)

}
