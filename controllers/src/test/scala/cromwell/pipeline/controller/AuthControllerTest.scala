package cromwell.pipeline.controller

import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.{ HttpEntity, StatusCodes }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.controller.AuthController._
import cromwell.pipeline.datastorage.dto.auth.{ AuthResponse, SignInRequest, SignUpRequest }
import cromwell.pipeline.service.AuthService
import cromwell.pipeline.service.AuthorizationException.{
  DuplicateUserException,
  InactiveUserException,
  IncorrectPasswordException,
  RegistrationFailureException,
  UserNotFoundException
}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Assertion, Matchers, WordSpec }

import scala.concurrent.Future

class AuthControllerTest extends WordSpec with Matchers with MockFactory with ScalatestRouteTest {

  private val authService = stub[AuthService]
  private val authController = new AuthController(authService)
  private val accessToken = "access-token"
  private val refreshToken = "refresh-token"
  private val accessTokenExpiration = 300
  private val email = "JohnDoe@cromwell.com"
  private val password = "Password_213"
  private val incorrectPassword = "Password_2134"
  private val firstName = "FirstName"
  private val lastName = "LastName"

  "AuthController" when {

    "signIn" should {

      "return token headers if user exists" taggedAs Controller in {
        val authResponse = AuthResponse(accessToken, refreshToken, accessTokenExpiration)
        val signInRequestStr = s"""{"email":"${email}","password":"${password}"}"""

        val httpEntity = HttpEntity(`application/json`, signInRequestStr)
        (authService.signIn _ when SignInRequest(email, password)).returns(Future.successful(Some(authResponse)))
        Post("/auth/signIn", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.OK
          checkAuthTokens
        }
      }

      "return Unauthorized status if user doesn't exist" taggedAs Controller in {
        val signInRequestStr = s"""{"email":"${email}","password":"${password}"}"""
        val httpEntity = HttpEntity(`application/json`, signInRequestStr)
        (authService.signIn _ when SignInRequest(email, password))
          .returns(Future.failed(UserNotFoundException("user has not been found")))

        Post("/auth/signIn", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }

      "return Unauthorized when password is incorrect" taggedAs Controller in {
        val signInRequestStr = s"""{"email":"${email}","password":"${incorrectPassword}"}"""
        val httpEntity = HttpEntity(`application/json`, signInRequestStr)
        (authService.signIn _ when SignInRequest(email, incorrectPassword))
          .returns(Future.failed(IncorrectPasswordException("incorrect password")))
        Post("/auth/signIn", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }

      "return Unauthorized status if user is not active" taggedAs Controller in {
        val signInRequestStr = s"""{"email":"${email}","password":"${password}"}"""
        val httpEntity = HttpEntity(`application/json`, signInRequestStr)
        (authService.signIn _ when SignInRequest(email, password))
          .returns(Future.failed(InactiveUserException("user is not active")))

        Post("/auth/signIn", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }
    }

    "signUp" should {

      "return token headers if user was successfully registered" taggedAs Controller in {
        val signInRequestStr =
          s"""{"email":"${email}","password":"${password}","firstName":"${firstName}","lastName":"${lastName}"}"""
        val authResponse = AuthResponse(accessToken, refreshToken, accessTokenExpiration)
        val httpEntity = HttpEntity(`application/json`, signInRequestStr)
        (authService.signUp _ when SignUpRequest(email, password, firstName, lastName))
          .returns(Future.successful(Some(authResponse)))

        Post("/auth/signUp", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.OK
          checkAuthTokens
        }
      }

      "return BadRequest with fields validation errors" taggedAs Controller in {
        val signUpRequestStr =
          s"""{"email":"${email}","password":"${password}","firstName":"${firstName}","lastName":"${lastName}"}"""
        val authResponse = AuthResponse(accessToken, refreshToken, accessTokenExpiration)
        val httpEntity = HttpEntity(`application/json`, signUpRequestStr)
        (authService.signUp _ when SignUpRequest(email, password, firstName, lastName))
          .returns(Future.failed(RegistrationFailureException("")))

        Post("/auth/signUp", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }

      "return BadRequest status if user registration was failed" taggedAs Controller in {
        val signUpRequestStr =
          s"""{"email":"${email}","password":"${password}","firstName":"${firstName}","lastName":"${lastName}"}"""
        val httpEntity = HttpEntity(`application/json`, signUpRequestStr)
        (authService.signUp _ when SignUpRequest(email, password, firstName, lastName))
          .returns(Future(throw new RuntimeException("Something wrong.")))

        Post("/auth/signUp", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }

      "return BadRequest status if user already exists" taggedAs Controller in {
        val signUpRequestStr =
          s"""{"email":"${email}","password":"${password}","firstName":"${firstName}","lastName":"${lastName}"}"""
        val httpEntity = HttpEntity(`application/json`, signUpRequestStr)
        (authService.signUp _ when SignUpRequest(email, password, firstName, lastName))
          .returns(Future.failed(DuplicateUserException(s"${email} already exists")))

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
