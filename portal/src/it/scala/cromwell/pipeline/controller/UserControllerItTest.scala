package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.ApplicationComponents
import cromwell.pipeline.datastorage.dto.{ User, UserNoCredentials }
import cromwell.pipeline.utils.auth.{ AccessTokenContent, TestContainersUtils, TestUserUtils }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import org.scalatest.{ AsyncWordSpec, Matchers }

class UserControllerItTest extends AsyncWordSpec with Matchers with ScalatestRouteTest with ForAllTestContainer {
  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  container.start()
  implicit val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  private val components: ApplicationComponents = new ApplicationComponents()

  override protected def beforeAll(): Unit =
    components.datastorageModule.pipelineDatabaseEngine.updateSchema()

  import components.controllerModule.userController
  import components.datastorageModule.userRepository

  "UserController" when {
    "deactivateByEmail" should {
      "return user's entity with false value if user was successfully deactivated" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        userRepository.addUser(dummyUser).map { _ =>
          val deactivateUserByEmailRequest = dummyUser.email
          val accessToken = AccessTokenContent(dummyUser.userId.value)
          Delete("/users/delete", deactivateUserByEmailRequest) ~> userController.route(accessToken) ~> check {
            val deactivatedUserResponse = UserNoCredentials.fromUser(dummyUser.copy(active = false))
            responseAs[UserNoCredentials] shouldBe deactivatedUserResponse
            status shouldBe StatusCodes.OK
          }
        }
      }
    }
    "deactivateById" should {
      "return user's entity with false value if user was successfully deactivated" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        userRepository.addUser(dummyUser).map { _ =>
          val accessToken = AccessTokenContent(dummyUser.userId.value)
          Delete("/users/delete") ~> userController.route(accessToken) ~> check {
            val deactivatedUserResponse = UserNoCredentials.fromUser(dummyUser.copy(active = false))
            responseAs[UserNoCredentials] shouldBe deactivatedUserResponse
            status shouldBe StatusCodes.OK
          }
        }
      }
    }
  }
}
