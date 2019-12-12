package cromwell.pipeline.controller

import java.util.UUID

import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.{ HttpEntity, StatusCodes }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.datastorage.dto.user.DeactivateUserRequestByEmail
import cromwell.pipeline.datastorage.dto.{ User, UserDeactivationByEmailResponse, UserDeactivationByIdResponse, UserId }
import cromwell.pipeline.utils.StringUtils
import cromwell.pipeline.{ ApplicationComponents, TestContainersUtils }
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.{ Matchers, WordSpec }
import play.api.libs.json.Json

class UserControllerItTest extends WordSpec with Matchers with ScalatestRouteTest with ForAllTestContainer {
  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  container.start()
  implicit val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  private val components: ApplicationComponents = new ApplicationComponents()

  override protected def beforeAll(): Unit =
    components.datastorageModule.pipelineDatabaseEngine.updateSchema()

  import components.controllerModule.userController
  import components.datastorageModule.userRepository

  private val userPassword = "-Pa$$w0rd-"

  "UserController" when {
    "deactivateByEmail" should {
      "return email and false value if user was successfully deactivated" in {
        val newUser = getDummyUser(userPassword)
        val emailResponse = UserDeactivationByEmailResponse(newUser.email, false)
        whenReady(userRepository.addUser(newUser)) { _ =>
          val deactivateUserRequestByEmail = DeactivateUserRequestByEmail(newUser.email)
          val httpEntity = HttpEntity(`application/json`, Json.stringify(Json.toJson(deactivateUserRequestByEmail)))
          Delete("/users/deactivate", httpEntity) ~> userController.route ~> check {
            val response = Json.parse(responseAs[String]).as[UserDeactivationByEmailResponse]
            response shouldBe emailResponse
            status shouldBe StatusCodes.OK
          }
        }
      }
    }
    "deactivateById" should {
      "return id and false value if user was successfully deactivated" in {
        val newUser = getDummyUser(userPassword)
        val idResponse = UserDeactivationByIdResponse(newUser.userId, false)
        whenReady(userRepository.addUser(newUser)) { _ =>
          Delete(s"/users/deactivate/${newUser.userId.value}") ~> userController.route ~> check {
            val response = Json.parse(responseAs[String]).as[UserDeactivationByIdResponse]
            response shouldBe idResponse
            status shouldBe StatusCodes.OK
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
}
