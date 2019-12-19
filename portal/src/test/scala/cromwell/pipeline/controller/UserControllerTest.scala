package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dto.{ User, UserId, UserNoCredentials }
import cromwell.pipeline.service.UserService
import cromwell.pipeline.utils.auth.{ SecurityDirective, TestUserUtils }
import cromwell.pipeline.{ AuthConfig, ExpirationTimeInSeconds }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import org.mockito.Mockito._
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar
import pdi.jwt.JwtAlgorithm

import scala.concurrent.Future

class UserControllerTest extends AsyncWordSpec with Matchers with MockitoSugar with ScalatestRouteTest {

  private val userService = mock[UserService]
  private val userController = new UserController(userService)

  "UserController" when {
    "deactivateByEmail" should {
      "return email and false value if user was successfully deactivated" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val deactivateUserByEmailRequest = "JohnDoe@cromwell.com"
        val response = UserNoCredentials.fromUser(dummyUser)

        when(userService.deactivateByEmail(deactivateUserByEmailRequest)).thenReturn(Future(Option(response)))

        Delete("/users/deactivate", deactivateUserByEmailRequest) ~> userController.route ~> check {
          responseAs[UserNoCredentials] shouldBe response
          status shouldBe StatusCodes.OK
        }
      }
      "return server error if user deactivation was failed" in {
        val deactivateUserByEmailRequest = "JohnDoe@cromwell.com"

        when(userService.deactivateByEmail(deactivateUserByEmailRequest))
          .thenReturn(Future.failed(new RuntimeException("Something wrong.")))

        Delete("/users/deactivate", deactivateUserByEmailRequest) ~> userController.route ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
      "return BadRequest status if user deactivation was failed" in {
        val deactivateUserByEmailRequest = "JohnDoe@cromwell.com"

        when(userService.deactivateByEmail(deactivateUserByEmailRequest)).thenReturn(Future(None))

        Delete("/users/deactivate", deactivateUserByEmailRequest) ~> userController.route ~> check {
          status shouldBe StatusCodes.NotFound
        }
      }
    }
    "deactivateById" should {
      "return id and false value if user was successfully deactivated" in {
        val dummyUser: User = TestUserUtils.getDummyUser(active = false)
        val userId = TestUserUtils.getDummyUser().userId.value
        val response = UserNoCredentials.fromUser(dummyUser)

        when(userService.deactivateById(UserId(userId))).thenReturn(Future(Option(response)))

        Delete(s"/users/deactivate/$userId") ~> userController.route ~> check {
          responseAs[UserNoCredentials] shouldBe response
          status shouldBe StatusCodes.OK
        }
      }
      "return server error if user deactivation was failed" in {
        val userId = TestUserUtils.getDummyUser().userId.value
        when(userService.deactivateById(UserId(userId)))
          .thenReturn(Future.failed(new RuntimeException("Something wrong.")))

        Delete(s"/users/deactivate/$userId") ~> userController.route ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
      "return BabRequest status if user deactivation was failed" in {
        val userId = TestUserUtils.getDummyUser().userId.value
        when(userService.deactivateById(UserId(userId))).thenReturn(Future(None))

        Delete(s"/users/deactivate/$userId") ~> userController.route ~> check {
          status shouldBe StatusCodes.NotFound
        }
      }
      "should return error when user's not found" in {
        val deactivateUserRequestByEmail = "JohnDoe@cromwell.com"
        when(userService.deactivateByEmail(deactivateUserRequestByEmail)).thenReturn(Future(None))
        Delete("/users/deactivate", deactivateUserRequestByEmail) ~> userController.route ~> check {
          status shouldBe StatusCodes.NotFound
        }
      }
      "should return error when we're not signed in" in {
        val fakeAuthCfg = AuthConfig("123", JwtAlgorithm.HS384, ExpirationTimeInSeconds(1, 1, 1))
        val securityDirective = new SecurityDirective(fakeAuthCfg)
        val deactivateUserByEmailRequest = "JohnDoe@cromwell.com"
        when(userService.deactivateByEmail(deactivateUserByEmailRequest)).thenReturn(Future(None))
        Delete("/users/deactivate", deactivateUserByEmailRequest) ~> securityDirective.authenticated { _ =>
          userController.route
        } ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }
      "should return err when id is not 36 symbols" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val userId = "not-user-id"
        val response = UserNoCredentials.fromUser(dummyUser)
        when(userService.deactivateById(UserId(userId))).thenReturn(Future(Option(response)))
        Delete(s"/users/deactivate/$userId") ~> userController.route ~> check {
          handled shouldBe false
        }
      }
    }
  }
}
