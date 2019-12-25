package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dto.{ User, UserId, UserNoCredentials }
import cromwell.pipeline.service.UserService
import cromwell.pipeline.utils.auth.{ AccessTokenContent, TestUserUtils }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import org.mockito.Mockito._
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class UserControllerTest extends AsyncWordSpec with Matchers with MockitoSugar with ScalatestRouteTest {

  private val userService = mock[UserService]
  private val userController = new UserController(userService)

  "UserController" when {
    "deactivateById" should {
      "return user's entity with false value if user was successfully deactivated" in {
        val dummyUser: User = TestUserUtils.getDummyUser(active = false)
        val userId = dummyUser.userId
        val response = UserNoCredentials.fromUser(dummyUser)
        val accessToken = AccessTokenContent(userId.value)

        when(userService.deactivateById(userId)).thenReturn(Future.successful(Some(response)))

        Delete("/users/delete") ~> userController.route(accessToken) ~> check {
          responseAs[UserNoCredentials] shouldBe response
          status shouldBe StatusCodes.OK
        }
      }
      "return server error if user deactivation was failed" in {
        val userId = TestUserUtils.getDummyUser().userId.value
        val accessToken = AccessTokenContent(userId)
        when(userService.deactivateById(UserId(userId)))
          .thenReturn(Future.failed(new RuntimeException("Something wrong.")))

        Delete("/users/delete") ~> userController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
      "return NotFound status if user deactivation was failed" in {
        val userId = TestUserUtils.getDummyUser().userId.value
        val accessToken = AccessTokenContent(userId)
        when(userService.deactivateById(UserId(userId))).thenReturn(Future(None))

        Delete("/users/delete") ~> userController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NotFound
        }
      }
    }
  }
}
