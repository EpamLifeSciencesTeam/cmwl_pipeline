package cromwell.pipeline.controller

import java.util.UUID

import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.{ HttpEntity, StatusCodes }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.controller.AuthController._
import cromwell.pipeline.datastorage.dto.auth.{ SignInRequest, SignUpRequest }
import cromwell.pipeline.datastorage.dto.{ User, UserId }
import cromwell.pipeline.utils.StringUtils
import cromwell.pipeline.{ ApplicationComponents, TestContainersUtils }
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

  import components.datastorageModule.userRepository
  import components.controllerModule.authController

  private val userPassword = "-Pa$$w0rd-"

  "AuthController" when {

    "signIn" should {

      "return token headers if user exists" in {
        val newUser = getDummyUser(userPassword)

        whenReady(userRepository.addUser(newUser)) { _ =>
          val signInRequest = SignInRequest(newUser.email, userPassword)
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
        val newUser = getDummyUser(userPassword)
        val signUpRequest = SignUpRequest(newUser.email, userPassword, newUser.firstName, newUser.lastName)
        val httpEntity = HttpEntity(`application/json`, Json.stringify(Json.toJson(signUpRequest)))

        Post("/auth/signUp", httpEntity) ~> authController.route ~> check {
          status shouldBe StatusCodes.OK
          checkAuthTokens
        }
      }
    }

    "refresh" should {

      "return updated token headers if refresh token was valid and active" in {
        val newUser = getDummyUser(userPassword)

        whenReady(userRepository.addUser(newUser)) { _ =>
          val signInRequest = SignInRequest(newUser.email, userPassword)
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

  private def getDummyUser(password: String = userPassword, passwordSalt: String = "salt"): User = {
    val uuid = UUID.randomUUID().toString
    val passwordHash = StringUtils.calculatePasswordHash(password, passwordSalt)
    User(
      userId = UserId(uuid),
      email = s"JohnDoe-$uuid@cromwell.com",
      passwordHash = passwordHash,
      passwordSalt = passwordSalt,
      firstName = "FirstName",
      lastName = "LastName",
      profilePicture = None
    )
  }

  private def checkAuthTokens: Assertion =
    Seq(AccessTokenHeader, RefreshTokenHeader, AccessTokenExpirationHeader).forall(header(_).isDefined) shouldBe true

}
