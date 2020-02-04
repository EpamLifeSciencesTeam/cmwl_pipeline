package cromwell.pipeline.controller

import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.{ HttpEntity, StatusCodes }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.implicits._
import cromwell.pipeline.controller.AuthController._
import cromwell.pipeline.datastorage.dto.auth.{ AuthResponse, Password, SignInRequest, SignUpRequest }
import cromwell.pipeline.datastorage.dto.{ Name, UserEmail }
import cromwell.pipeline.service.AuthService
import cromwell.pipeline.utils.auth.TestUserUtils
import cromwell.pipeline.utils.validator.Enable.Unsafe
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Assertion, Matchers, WordSpec }
import play.api.libs.json.Json

import scala.concurrent.Future

class AuthControllerTest extends WordSpec with Matchers with MockFactory with ScalatestRouteTest {

  private val authService = stub[AuthService]
  private val authController = new AuthController(authService)
  private val accessToken = "access-token"
  private val refreshToken = "refresh-token"
  private val accessTokenExpiration = 300
  private val password = TestUserUtils.userPassword

  "AuthController" when {

    "signIn" should {

      "return token headers if user exists" in {
        val signInRequest = SignInRequest(UserEmail("email@cromwell.com"), password)
        val authResponse = AuthResponse(accessToken, refreshToken, accessTokenExpiration)
        val httpEntity = HttpEntity(`application/json`, Json.stringify(Json.toJson(signInRequest)))
        (authService.signIn _ when signInRequest).returns(Future(Option(authResponse)))

        Post("/auth/signIn", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.OK
          checkAuthTokens
        }
      }

      "return Unauthorized status if user doesn't exist" in {
        val signInRequest = SignInRequest(UserEmail("email@cromwell.com"), password)
        val httpEntity = HttpEntity(`application/json`, Json.stringify(Json.toJson(signInRequest)))
        (authService.signIn _ when signInRequest).returns(Future(None))

        Post("/auth/signIn", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }
    }

    "signUp" should {

      "return token headers if user was successfully registered" in {
        val signUpRequest = SignUpRequest(
          UserEmail("JohnDoe@cromwell.com"),
          Password("Password213"),
          Name("FirstName"),
          Name("LastName")
        )
        val authResponse = AuthResponse(accessToken, refreshToken, accessTokenExpiration)
        val httpEntity = HttpEntity(`application/json`, Json.stringify(Json.toJson(signUpRequest)))
        (authService.signUp _ when signUpRequest).returns(Future(Some(authResponse)))

        Post("/auth/signUp", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.OK
          checkAuthTokens
        }
      }

      // Info: Create custom exception handlers for an invalid user's field.
      // So far StatusCodes.BadRequest is returned but we want a returned entity
      // to be in Json format.
      // You can find this task in Jira with ticket: EPMLSTRCMW-124.

      "return BadRequest with fields validation errors" ignore {
        val signUpRequest = SignUpRequest(
          UserEmail("JohnDoe@cromwell.com"),
          Password("Password213"),
          Name("FirstName"),
          Name("LastName")
        )
        val signUpJson = Json.stringify(Json.toJson(signUpRequest))
        val authResponse = AuthResponse(accessToken, refreshToken, accessTokenExpiration)

        val httpEntity = HttpEntity(`application/json`, signUpJson)
        (authService.signUp _ when signUpRequest).returns(Future(Some(authResponse)))

        Post("/auth/signUp", httpEntity) ~> authController.route ~> check {

          status shouldBe StatusCodes.BadRequest
        }
      }

      "return BadRequest status if user registration was failed" in {
        val signUpRequest = SignUpRequest(
          UserEmail("JohnDoe@cromwell.com"),
          Password("Password213"),
          Name("FirstName"),
          Name("LastName")
        )
        val httpEntity = HttpEntity(`application/json`, Json.stringify(Json.toJson(signUpRequest)))
        (authService.signUp _ when signUpRequest).returns(Future(throw new RuntimeException("Something wrong.")))

        Post("/auth/signUp", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
    }

    "refresh" should {

      "return updated token headers if refresh token was valid and active" in {
        val authResponse = AuthResponse(accessToken, refreshToken, accessTokenExpiration)
        (authService.refreshTokens _ when refreshToken).returns(Some(authResponse))

        Get(s"/auth/refresh?refreshToken=$refreshToken") ~> authController.route ~> check {
          status shouldBe StatusCodes.OK
          checkAuthTokens
        }
      }

      "return BadRequest status if something was wrong with refresh token" in {
        (authService.refreshTokens _ when refreshToken).returns(None)

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
