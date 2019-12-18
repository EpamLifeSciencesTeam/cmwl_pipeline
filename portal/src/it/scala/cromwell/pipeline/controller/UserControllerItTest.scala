package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.ApplicationComponents
import cromwell.pipeline.datastorage.dto.{ User, UserDeactivationResponse }
import cromwell.pipeline.utils.auth.{ TestContainersUtils, TestUserUtils }
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
      "return email and false value if user was successfully deactivated" in {
        val dummyUser: User = TestUserUtils.getDummyUser(active = false)
        val response = UserDeactivationResponse.fromUser(dummyUser)
        userRepository.addUser(dummyUser).map { _ =>
          val deactivateUserByEmailRequest = dummyUser.email
          Delete("/users/deactivate", deactivateUserByEmailRequest) ~> userController.route ~> check {
            responseAs[UserDeactivationResponse] shouldBe response
            status shouldBe StatusCodes.OK
          }
        }
      }
    }
    "deactivateById" should {
      "return id and false value if user was successfully deactivated" in {
        val dummyUser: User = TestUserUtils.getDummyUser(active = false)
        val response = UserDeactivationResponse.fromUser(dummyUser)
        userRepository.addUser(dummyUser).map { _ =>
          Delete(s"/users/deactivate/${dummyUser.userId.value}") ~> userController.route ~> check {
            responseAs[UserDeactivationResponse] shouldBe response
            status shouldBe StatusCodes.OK
          }
        }
      }
    }
  }
}
