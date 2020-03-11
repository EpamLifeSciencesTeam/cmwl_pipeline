package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dao.repository.utils.TestUserUtils
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.datastorage.dto.{ User, UserId, UserNoCredentials }
import cromwell.pipeline.datastorage.utils.auth.AccessTokenContent
import cromwell.pipeline.service.UserService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
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

  private val userService = mock[UserService]
  private val userController = new UserController(userService)
  private val dummyUser: User = TestUserUtils.getDummyUser()

  "UserController" when {

    "get users by email" should {

      "return the sequence of users" taggedAs Controller in {
        val usersByEmailRequest: String = "@mail"
        val userId = dummyUser.userId
        val uEmailRespSeq: Seq[User] = Seq(dummyUser)

        val accessToken = AccessTokenContent(userId.value)
        when(userService.getUsersByEmail(usersByEmailRequest)).thenReturn(Future.successful(uEmailRespSeq))

        Get("/users?email=" + usersByEmailRequest) ~> userController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Seq[User]] shouldEqual uEmailRespSeq
          responseAs[Seq[User]].size shouldEqual 1
        }
      }
      "return the internal server error if service fails" taggedAs Controller in {
        val usersByEmailRequest: String = "@mail"
        val accessToken = AccessTokenContent(dummyUser.userId.value)
        when(userService.getUsersByEmail(usersByEmailRequest))
          .thenReturn(Future.failed(new RuntimeException("something went wrong")))

        Get("/users?email=" + usersByEmailRequest) ~> userController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
      "return the sequence of users when pattern must contain correct number of entries" taggedAs Controller in {
        val usersByEmailRequest: String = "someDomain.com"
        val userId = dummyUser.userId

        val firstDummyUser: User =
          dummyUser.copy(email = usersByEmailRequest)
        val secondDummyUser: User =
          dummyUser.copy(email = usersByEmailRequest)
        val uEmailRespSeq: Seq[User] = Seq(firstDummyUser, secondDummyUser)
        val accessToken = AccessTokenContent(userId.value)
        when(userService.getUsersByEmail(usersByEmailRequest)).thenReturn(Future.successful(uEmailRespSeq))

        Get("/users?email=" + usersByEmailRequest) ~> userController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Seq[User]] shouldEqual uEmailRespSeq
          responseAs[Seq[User]].size shouldEqual 2
        }
      }
    }

    "deactivateUserById" should {
      "return user's entity with false value if user was successfully deactivated" taggedAs Controller in {
        val userId = dummyUser.copy(active = false).userId
        val response = UserNoCredentials.fromUser(dummyUser)
        val accessToken = AccessTokenContent(userId.value)

        when(userService.deactivateUserById(userId)).thenReturn(Future.successful(Some(response)))

        Delete("/users") ~> userController.route(accessToken) ~> check {
          responseAs[UserNoCredentials] shouldBe response
          status shouldBe StatusCodes.OK
        }
      }
      "return server error if user deactivation was failed" taggedAs Controller in {
        val userId = dummyUser.userId.value
        val accessToken = AccessTokenContent(userId)
        when(userService.deactivateUserById(UserId(userId)))
          .thenReturn(Future.failed(new RuntimeException("Something wrong.")))

        Delete("/users") ~> userController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
      "return NotFound status if user deactivation was failed" taggedAs Controller in {
        val userId = dummyUser.userId.value
        val accessToken = AccessTokenContent(userId)
        when(userService.deactivateUserById(UserId(userId))).thenReturn(Future(None))

        Delete("/users") ~> userController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NotFound
        }
      }
    }

    "update" should {
      "return NoContent status if user was amended" taggedAs Controller in {
        val userId = dummyUser.userId.value
        val accessToken = AccessTokenContent(userId)
        val request = UserUpdateRequest(dummyUser.email, dummyUser.firstName, dummyUser.lastName)

        when(userService.updateUser(userId, request)).thenReturn(Future.successful(1))

        Put("/users", request) ~> userController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NoContent
        }
      }

      "return NoContent status if user's password was amended" taggedAs Controller in {
        val userId = dummyUser.userId.value
        val accessToken = AccessTokenContent(userId)
        val userPassword = "-Pa$$w0rd-"
        val request = PasswordUpdateRequest(userPassword, userPassword, userPassword)

        when(userService.updatePassword(userId, request)).thenReturn(Future.successful(1))

        Put("/users", request) ~> userController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NoContent
        }
      }

      "return InternalServerError status if user's id doesn't match" taggedAs Controller in {
        val userId = dummyUser.userId.value
        val accessToken = AccessTokenContent("0")
        val request = UserUpdateRequest(dummyUser.email, dummyUser.firstName, dummyUser.lastName)

        when(userService.updateUser(userId, request))
          .thenReturn(Future.failed(new RuntimeException("Something wrong.")))

        Put("/users", request) ~> userController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }

      "return BadRequest status if user's passwords don't match" taggedAs Controller in {
        val userId = dummyUser.userId.value
        val accessToken = AccessTokenContent(userId)
        val userPassword = "-Pa$$w0rd-"
        val request = PasswordUpdateRequest(userPassword, userPassword + "1", userPassword)

        when(userService.updatePassword(userId, request))
          .thenReturn(Future.failed(new RuntimeException("Something wrong.")))

        Put("/users", request) ~> userController.route(accessToken) ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
    }
  }
}
