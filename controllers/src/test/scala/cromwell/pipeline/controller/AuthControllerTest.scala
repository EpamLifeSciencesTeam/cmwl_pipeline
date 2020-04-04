package cromwell.pipeline.controller

import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.{ HttpEntity, StatusCodes }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.controller.AuthController._
import cromwell.pipeline.datastorage.dto.auth.{ AuthResponse, PasswordProblemsResponse, SignInRequest, SignUpRequest }
import cromwell.pipeline.datastorage.utils.validator.DomainValidation
import cromwell.pipeline.service.AuthService
import play.api.libs.json.Json
import cromwell.pipeline.datastorage.dto.formatters.AuthFormatters._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Assertion, Matchers, WordSpec }

import scala.concurrent.Future

class AuthControllerTest extends WordSpec with Matchers with MockFactory with ScalatestRouteTest {

  private val authService = stub[AuthService]
  private val authController = new AuthController(authService)
  private val accessToken = "access-token"
  private val refreshToken = "refresh-token"
  private val accessTokenExpiration = 300

  "AuthController" when {

    "signIn" should {

      "return token headers if user exists" taggedAs Controller in {
        val email = "email@cromwell.com"
        val password = "password"
        val authResponse = AuthResponse(accessToken, refreshToken, accessTokenExpiration)
        val signInRequestStr = s"""{"email":"${email}","password":"${password}"}"""
        val httpEntity = HttpEntity(`application/json`, signInRequestStr)
        (authService.signIn _ when SignInRequest(email, password)).returns(Future(Option(authResponse)))

        Post("/auth/signIn", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.OK
          checkAuthTokens
        }
      }

      "return Unauthorized status if user doesn't exist" taggedAs Controller in {
        val email = "email@cromwell.com"
        val password = "password"
        val signInRequestStr = s"""{"email":"${email}","password":"${password}"}"""
        val httpEntity = HttpEntity(`application/json`, signInRequestStr)
        (authService.signIn _ when SignInRequest(email, password)).returns(Future(None))

        Post("/auth/signIn", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }
    }

    "signUp" should {

      "return token headers if user was successfully registered" taggedAs Controller in {
        val email = "JohnDoe@cromwell.com"
        val password = "Password213"
        val firstName = "FirstName"
        val lastName = "LastName"
        val signInRequestStr =
          s"""{"email":"${email}","password":"${password}","firstName":"${firstName}","lastName":"${lastName}"}"""
        val authResponse = AuthResponse(accessToken, refreshToken, accessTokenExpiration)
        val httpEntity = HttpEntity(`application/json`, signInRequestStr)
        (authService.signUp _ when SignUpRequest(email, password, firstName, lastName))
          .returns(Future(Some(authResponse)))

        Post("/auth/signUp", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.OK
          checkAuthTokens
        }
      }

      "return BadRequest with fields validation errors" taggedAs Controller in {
        val email = "email"
        val password = "password"
        val firstName = "First-name"
        val lastName = "Last-name"
        val signUpRequestStr =
          s"""{"email":"${email}","password":"${password}","firstName":"${firstName}","lastName":"${lastName}"}"""
        val authResponse = AuthResponse(accessToken, refreshToken, accessTokenExpiration)

        val httpEntity = HttpEntity(`application/json`, signUpRequestStr)
        (authService.signUp _ when SignUpRequest(email, password, firstName, lastName))
          .returns(Future(Some(authResponse)))

        Post("/auth/signUp", httpEntity) ~> authController.route ~> check {
          val response = Json.parse(responseAs[String]).as[PasswordProblemsResponse]
          val errorCodes = response.errors.map(_("errorCode")).toSet

          status shouldBe StatusCodes.BadRequest
          DomainValidation.allErrorCodes.forall(errorCodes.contains) shouldBe true
        }
      }

      "return BadRequest status if user registration was failed" taggedAs Controller in {
        val email = "email@cromwell.com"
        val password = "password"
        val firstName = "First-name"
        val lastName = "Last-name"
        val signUpRequestStr =
          s"""{"email":"${email}","password":"${password}","firstName":"${firstName}","lastName":"${lastName}"}"""
        val httpEntity = HttpEntity(`application/json`, signUpRequestStr)
        (authService.signUp _ when SignUpRequest(email, password, firstName, lastName))
          .returns(Future(throw new RuntimeException("Something wrong.")))

        Post("/auth/signUp", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
    }

    "refresh" should {

      "return updated token headers if refresh token was valid and active" taggedAs Controller in {
        val authResponse = AuthResponse(accessToken, refreshToken, accessTokenExpiration)
        (authService.refreshTokens _ when refreshToken).returns(Some(authResponse))

        Get(s"/auth/refresh?refreshToken=$refreshToken") ~> authController.route ~> check {
          status shouldBe StatusCodes.OK
          checkAuthTokens
        }
      }

      "return BadRequest status if something was wrong with refresh token" taggedAs Controller in {
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
