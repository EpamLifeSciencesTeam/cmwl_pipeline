package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.implicits._
import cromwell.pipeline.datastorage.dao.utils.TestUserUtils
import cromwell.pipeline.datastorage.dto.User
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.model.validator.Enable
import cromwell.pipeline.model.wrapper.{ Password, UserEmail, UserId }
import cromwell.pipeline.service.UserService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.mockito.Mockito._
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class UserControllerTest
    extends AsyncWordSpec
    with Matchers
    with ScalatestRouteTest
    with MockitoSugar
    with BeforeAndAfterAll {
  import PlayJsonSupport._

  private val userService = mock[UserService]
  private val userController = new UserController(userService)
  private val password: String = "-Pa$$w0rd1-"

  "UserController" when {

    "get users by email" should {

      "return the sequence of users" taggedAs Controller in {
        val usersByEmailRequest: UserEmail = UserEmail("someDomain@mail.com", Enable.Unsafe)
        val dummyUser = TestUserUtils.getDummyUser()
        val userId = dummyUser.userId
        val uEmailRespSeq: Seq[User] = Seq(dummyUser)

        val accessToken = AccessTokenContent(userId)
        when(userService.getUsersByEmail(usersByEmailRequest.unwrap)).thenReturn(Future.successful(uEmailRespSeq))
        Get("/users?email=" + usersByEmailRequest) ~> userController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Seq[User]] shouldEqual uEmailRespSeq
          responseAs[Seq[User]].size shouldEqual 1
        }
      }
      "return the internal server error if service fails" taggedAs Controller in {
        val usersByEmailRequest = "someDomain@mail.com"
        val userId = UserId.random
        val accessToken = AccessTokenContent(userId)
        when(userService.getUsersByEmail(usersByEmailRequest))
          .thenReturn(Future.failed(new RuntimeException("something went wrong")))

        Get("/users?email=" + usersByEmailRequest) ~> userController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
      "return the sequence of users when pattern must contain correct number of entries" taggedAs Controller in {
        val usersByEmailRequest: UserEmail = UserEmail("someDomain@mail.com", Enable.Unsafe)
        val dummyUser: User = TestUserUtils.getDummyUser()
        val userId = dummyUser.userId
        val firstDummyUser: User = TestUserUtils.getDummyUser()
        val secondDummyUser: User = TestUserUtils.getDummyUser()
        val uEmailRespSeq: Seq[User] = Seq(firstDummyUser, secondDummyUser)
        val accessToken = AccessTokenContent(userId)

        when(userService.getUsersByEmail(usersByEmailRequest.unwrap)).thenReturn(Future.successful(uEmailRespSeq))
        Get("/users?email=" + usersByEmailRequest) ~> userController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Seq[User]] shouldEqual uEmailRespSeq
          responseAs[Seq[User]].size shouldEqual 2
        }
      }
    }

    "deactivateUserById" should {
      "return user's entity with false value if user was successfully deactivated" taggedAs Controller in {
        val response = TestUserUtils.getDummyUser(active = false)
        val userId = response.userId
        val accessToken = AccessTokenContent(userId)

        when(userService.deactivateUserById(userId)).thenReturn(Future.successful(Some(response)))

        Delete("/users") ~> userController.route(accessToken) ~> check {
          responseAs[User] shouldBe response
          status shouldBe StatusCodes.OK
        }
      }
      "return server error if user deactivation was failed" taggedAs Controller in {
        val userId = UserId.random
        val accessToken = AccessTokenContent(userId)
        when(userService.deactivateUserById(userId)).thenReturn(Future.failed(new RuntimeException("Something wrong.")))

        Delete("/users") ~> userController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
      "return NotFound status if user deactivation was failed" taggedAs Controller in {
        val userId = UserId.random
        val accessToken = AccessTokenContent(userId)
        when(userService.deactivateUserById(userId)).thenReturn(Future(None))

        Delete("/users") ~> userController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NotFound
        }
      }
    }

    "update" should {
      "return NoContent status if user was amended" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val userId = UserId.random
        val accessToken = AccessTokenContent(userId)
        val request = UserUpdateRequest(dummyUser.email, dummyUser.firstName, dummyUser.lastName)

        when(userService.updateUser(userId, request)).thenReturn(Future.successful(1))

        Put("/users/info", request) ~> userController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NoContent
        }
      }

      "return NoContent status if user's password was amended" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val userId = dummyUser.userId
        val accessToken = AccessTokenContent(userId)
        val request = PasswordUpdateRequest(
          Password(password, Enable.Unsafe),
          Password(password, Enable.Unsafe),
          Password(password, Enable.Unsafe)
        )

        when(userService.updatePassword(userId, request)).thenReturn(Future.successful(1))

        Put("/users/password", request) ~> userController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NoContent
        }
      }

      "return InternalServerError status if user's id doesn't match" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val userId = dummyUser.userId
        val accessToken = AccessTokenContent(userId)
        val request = UserUpdateRequest(dummyUser.email, dummyUser.firstName, dummyUser.lastName)

        when(userService.updateUser(userId, request))
          .thenReturn(Future.failed(new RuntimeException("Something wrong.")))

        Put("/users/info", request) ~> userController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }

      "return BadRequest status if user's passwords don't match" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val userId = dummyUser.userId
        val accessToken = AccessTokenContent(userId)
        val request =
          PasswordUpdateRequest(
            Password(password, Enable.Unsafe),
            Password(password + "1", Enable.Unsafe),
            Password(password, Enable.Unsafe)
          )

        when(userService.updatePassword(userId, request))
          .thenReturn(Future.failed(new RuntimeException("Something wrong.")))

        Put("/users/password", request) ~> userController.route(accessToken) ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
    }
  }
}
