package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.ApplicationComponents
import cromwell.pipeline.datastorage.dao.repository.utils.TestUserUtils
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.datastorage.dto.{ User, UserNoCredentials }
import cromwell.pipeline.utils.TestContainersUtils
import cromwell.pipeline.utils.auth.AccessTokenContent
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{ AsyncWordSpec, Matchers }
import cats.implicits._
import cromwell.pipeline.model.validator.Enable
import cromwell.pipeline.model.wrapper.Password

class UserControllerItTest
    extends AsyncWordSpec
    with Matchers
    with ScalatestRouteTest
    with PlayJsonSupport
    with ForAllTestContainer {

  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  private implicit lazy val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  private lazy val components: ApplicationComponents = new ApplicationComponents()

  import components.controllerModule.userController
  import components.datastorageModule.userRepository

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    components.datastorageModule.pipelineDatabaseEngine.updateSchema()
  }

  private val password: String = "-Pa$$w0rd-"

  "UserController" when {

    "getUsersByEmail" should {

      "should find newly added user by email pattern" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val userByEmailRequest: String = dummyUser.email.unwrap
        val seqUser: Seq[User] = Seq(dummyUser)
        userRepository.addUser(dummyUser).map { _ =>
          val accessToken = AccessTokenContent(dummyUser.userId)
          Get("/users?email=" + userByEmailRequest) ~> userController.route(accessToken) ~> check {
            status shouldBe StatusCodes.OK
            responseAs[Seq[User]] shouldEqual seqUser
          }
        }
      }
    }

    "deactivateUserById" should {

      "return user's entity with false value if user was successfully deactivated" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val deactivatedUserResponse = UserNoCredentials.fromUser(dummyUser.copy(active = false))
        userRepository.addUser(dummyUser).map { _ =>
          val accessToken = AccessTokenContent(dummyUser.userId)
          Delete("/users") ~> userController.route(accessToken) ~> check {
            responseAs[UserNoCredentials] shouldBe deactivatedUserResponse
            status shouldBe StatusCodes.OK
          }
        }
      }
    }

    "updateUser" should {

      "return status code NoContent if user was successfully updated" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val request = UserUpdateRequest(dummyUser.email, dummyUser.firstName, dummyUser.lastName)
        userRepository.addUser(dummyUser).flatMap { _ =>
          userRepository.updateUser(dummyUser).map { _ =>
            val accessToken = AccessTokenContent(dummyUser.userId)
            Put("/users", request) ~> userController.route(accessToken) ~> check {
              status shouldBe StatusCodes.NoContent
            }
          }
        }
      }
    }

    "updatePassword" should {

      "return status code NoContent if user's password was successfully updated" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val request =
          PasswordUpdateRequest(
            Password(password, Enable.Unsafe),
            Password("new$Password1", Enable.Unsafe),
            Password("new$Password1", Enable.Unsafe)
          )
        userRepository.addUser(dummyUser).flatMap { _ =>
          userRepository.updatePassword(dummyUser).map { _ =>
            val accessToken = AccessTokenContent(dummyUser.userId)
            Put("/users", request) ~> userController.route(accessToken) ~> check {
              status shouldBe StatusCodes.NoContent
            }
          }
        }
      }
    }
  }
}
