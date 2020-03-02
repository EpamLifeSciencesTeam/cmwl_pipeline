package cromwell.pipeline.controller

import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.{ HttpEntity, StatusCodes }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.ApplicationComponents
import cromwell.pipeline.controller.AuthController._
import cromwell.pipeline.datastorage.dao.repository.utils.TestUserUtils
import cromwell.pipeline.datastorage.dto.User
import cromwell.pipeline.utils.TestContainersUtils
import org.scalatest.compatible.Assertion
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.{ Matchers, WordSpec }

class AuthControllerItTest extends WordSpec with Matchers with ScalatestRouteTest with ForAllTestContainer {

  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  private implicit lazy val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  private lazy val components: ApplicationComponents = new ApplicationComponents()

  import components.controllerModule.authController
  import components.datastorageModule.userRepository

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    components.datastorageModule.pipelineDatabaseEngine.updateSchema()
  }

  private val dummyUser: User = TestUserUtils.getDummyUser()
  private val password: String = "-Pa$$w0rd-"
  private val email: String = "AnotherJohnDoe-@cromwell.com"

  "AuthController" when {

    "signIn" should {

      "return token headers if user exists" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        whenReady(userRepository.addUser(dummyUser)) { _ =>
          val signInRequestStr = s"""{"email":"${dummyUser.email}","password":"${password}"}"""
          val httpEntity = HttpEntity(`application/json`, signInRequestStr)
          Post("/auth/signIn", httpEntity) ~> authController.route ~> check {
            status shouldBe StatusCodes.OK
            checkAuthTokens
          }
        }
      }
    }

    "signUp" should {

      "return token headers if user was successfully registered" in {
        val signInRequestStr =
          s"""{"email":"${email}","password":"${password}","firstName":"${dummyUser.firstName}","lastName":"${dummyUser.lastName}"}"""
        val httpEntity = HttpEntity(`application/json`, signInRequestStr)
        Post("/auth/signUp", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.OK
          checkAuthTokens
        }
      }
    }

    "refresh" should {

      "return updated token headers if refresh token was valid and active" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        whenReady(userRepository.addUser(dummyUser)) { _ =>
          val signInRequestStr = s"""{"email":"${dummyUser.email}","password":"${password}"}"""
          val httpEntity = HttpEntity(`application/json`, signInRequestStr)
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
