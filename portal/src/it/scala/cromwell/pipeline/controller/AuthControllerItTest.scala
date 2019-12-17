package cromwell.pipeline.controller

import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.{ HttpEntity, StatusCodes }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.controller.AuthController._
import cromwell.pipeline.datastorage.dto.User
import cromwell.pipeline.datastorage.dto.auth.{ SignInRequest, SignUpRequest }
import cromwell.pipeline.utils.auth.{ TestContainersUtils, TestUserUtils }
import cromwell.pipeline.ApplicationComponents
import org.scalatest.compatible.Assertion
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.{ Matchers, WordSpec }
import play.api.libs.json.Json

class AuthControllerItTest extends WordSpec with Matchers with ScalatestRouteTest with ForAllTestContainer {

  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  container.start()
  implicit val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  private val components: ApplicationComponents = new ApplicationComponents()

  override protected def beforeAll(): Unit =
    components.datastorageModule.pipelineDatabaseEngine.updateSchema()

  import components.controllerModule.authController
  import components.datastorageModule.userRepository

  private val userPassword = "-Pa$$w0rd-"

  "AuthController" when {

    "signIn" should {

      "return token headers if user exists" in {
        val dummyUser: User = TestUserUtils.getDummyUser()

        whenReady(userRepository.addUser(dummyUser)) { _ =>
          val signInRequest = SignInRequest(dummyUser.email, userPassword)
          val httpEntity = HttpEntity(`application/json`, Json.stringify(Json.toJson(signInRequest)))

          Post("/auth/signIn", httpEntity) ~> authController.route ~> check {
            status shouldBe StatusCodes.OK
            checkAuthTokens
          }
        }
      }
    }

    "signUp" should {

      "return token headers if user was successfully registered" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val signUpRequest = SignUpRequest(dummyUser.email, userPassword, dummyUser.firstName, dummyUser.lastName)
        val httpEntity = HttpEntity(`application/json`, Json.stringify(Json.toJson(signUpRequest)))

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
          val signInRequest = SignInRequest(dummyUser.email, userPassword)
          val httpEntity = HttpEntity(`application/json`, Json.stringify(Json.toJson(signInRequest)))

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
