package cromwell.pipeline.controller

import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.{ HttpEntity, StatusCodes }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.ApplicationComponents
import cromwell.pipeline.controller.AuthController._
import cromwell.pipeline.datastorage.dao.utils.TestUserUtils
import cromwell.pipeline.datastorage.dao.utils.TestUserUtils._
import cromwell.pipeline.datastorage.dto.UserWithCredentials
import cromwell.pipeline.service.AuthService
import cromwell.pipeline.utils.TestContainersUtils
import org.scalatest.compatible.Assertion
import org.scalatest.{ Matchers, AsyncWordSpec }

class AuthControllerItTest extends AsyncWordSpec with Matchers with ScalatestRouteTest with ForAllTestContainer {

  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  private implicit lazy val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  private lazy val components: ApplicationComponents = new ApplicationComponents()

  import components.controllerModule.authController
  import components.datastorageModule.userRepository

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    components.datastorageModule.pipelineDatabaseEngine.updateSchema()
  }

  "AuthController" when {

    "signIn" should {

      "return token headers if user exists" in {
        val dummyUser: UserWithCredentials = TestUserUtils.getDummyUserWithCredentials()
        val signInRequestStr =
          s"""{"email":"${dummyUser.email}","password":"${userPassword}"}"""
        val httpEntity = HttpEntity(`application/json`, signInRequestStr)
        userRepository.addUser(dummyUser).map { _ =>
          Post("/auth/signIn", httpEntity) ~> authController.route ~> check {
            status shouldBe StatusCodes.OK
            checkAuthTokens
          }
        }
      }

      "return status Forbidden if user is inactive" in {
        val dummyUser: UserWithCredentials = TestUserUtils.getDummyUserWithCredentials(active = false)
        val signInRequestStr =
          s"""{"email":"${dummyUser.email}","password":"${userPassword}"}"""
        val httpEntity = HttpEntity(`application/json`, signInRequestStr)
        userRepository.addUser(dummyUser).map { _ =>
          Post("/auth/signIn", httpEntity) ~> authController.route ~> check {
            status shouldBe StatusCodes.Forbidden
            responseAs[String] shouldEqual "User is not active"
          }
        }
      }

      "return status Unauthorized if password is incorrect" in {
        val dummyUser: UserWithCredentials = TestUserUtils.getDummyUserWithCredentials(active = false)
        val incorrectPassword = dummyUser.email + "x"

        val signInRequestStr =
          s"""{"email":"${dummyUser.email}","password":"${incorrectPassword}"}"""
        val httpEntity = HttpEntity(`application/json`, signInRequestStr)

        userRepository.addUser(dummyUser).map { _ =>
          Post("/auth/signIn", httpEntity) ~> authController.route ~> check {
            status shouldBe StatusCodes.Unauthorized
            responseAs[String] shouldEqual AuthService.authorizationFailure
          }
        }
      }
    }

    "signUp" should {

      "return token headers if user was successfully registered" in {
        val dummyUser: UserWithCredentials = TestUserUtils.getDummyUserWithCredentials()
        val signUpRequestStr =
          s"""{"email":"${dummyUser.email}","password":"${userPassword}","firstName":"${dummyUser.firstName}","lastName":"${dummyUser.lastName}"}"""
        val httpEntity = HttpEntity(`application/json`, signUpRequestStr)
        Post("/auth/signUp", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.OK
          checkAuthTokens
        }
      }
    }

    "refresh" should {

      "return updated token headers if refresh token was valid and active" in {
        val dummyUser: UserWithCredentials = TestUserUtils.getDummyUserWithCredentials()
        val signInRequestStr =
          s"""{"email":"${dummyUser.email}","password":"${userPassword}"}"""
        val httpEntity = HttpEntity(`application/json`, signInRequestStr)
       userRepository.addUser(dummyUser).map { _ =>
          Post("/auth/signIn", httpEntity) ~> authController.route ~> check {
            header(RefreshTokenHeader).map { refreshTokenHeader =>
              Get(s"/auth/refresh?refreshToken=${refreshTokenHeader.value}") ~> authController.route ~> check {
                status shouldBe StatusCodes.OK
                checkAuthTokens
              }
            }.getOrElse(fail)
          }
        }
      }
    }
  }

  private def checkAuthTokens: Assertion =
    Seq(AccessTokenHeader, RefreshTokenHeader, AccessTokenExpirationHeader).forall(header(_).isDefined) shouldBe true
}
