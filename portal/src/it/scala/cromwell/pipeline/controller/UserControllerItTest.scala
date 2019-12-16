package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.datastorage.dto.{ User, UserDeactivationByEmailResponse, UserDeactivationByIdResponse }
import cromwell.pipeline.utils.auth.TestUserUtils
import cromwell.pipeline.{ ApplicationComponents, TestContainersUtils }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.{ Matchers, WordSpec }

class UserControllerItTest extends WordSpec with Matchers with ScalatestRouteTest with ForAllTestContainer {
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
        val dummyUser: User = TestUserUtils.getDummyUser()
        val emailResponse = UserDeactivationByEmailResponse(dummyUser.email, false)
        whenReady(userRepository.addUser(dummyUser)) { _ =>
          val deactivateUserByEmailRequest = dummyUser.email
          Delete("/users/deactivate", deactivateUserByEmailRequest) ~> userController.route ~> check {
            responseAs[UserDeactivationByEmailResponse] shouldBe emailResponse
            status shouldBe StatusCodes.OK
          }
        }
      }
    }
    "deactivateById" should {
      "return id and false value if user was successfully deactivated" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val idResponse = UserDeactivationByIdResponse(dummyUser.userId, false)
        whenReady(userRepository.addUser(dummyUser)) { _ =>
          Delete(s"/users/deactivate/${dummyUser.userId.value}") ~> userController.route ~> check {
            responseAs[UserDeactivationByIdResponse] shouldBe idResponse
            status shouldBe StatusCodes.OK
          }
        }
      }
    }
  }
}
