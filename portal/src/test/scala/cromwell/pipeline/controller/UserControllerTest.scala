package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dto.{ UserDeactivationByEmailResponse, UserDeactivationByIdResponse, UserId }
import cromwell.pipeline.service.UserService
import cromwell.pipeline.utils.auth.SecurityDirective
import cromwell.pipeline.{ AuthConfig, ExpirationTimeInSeconds }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Matchers, WordSpec }
import pdi.jwt.JwtAlgorithm

import scala.concurrent.Future

class UserControllerTest extends WordSpec with Matchers with MockFactory with ScalatestRouteTest {

  private val userService = stub[UserService]
  private val userController = new UserController(userService)

  "UserController" when {
    "deactivateByEmail" should {
      "return email and false value if user was successfully deactivated" in {
        val deactivateUserByEmailRequest = "email"
        val emailResponse = UserDeactivationByEmailResponse(email = "JohnDoe@cromwell.com", active = false)

        (userService.deactivateByEmail _ when deactivateUserByEmailRequest).returns(Future(Option(emailResponse)))

        Delete("/users/deactivate", deactivateUserByEmailRequest) ~> userController.route ~> check {
          responseAs[UserDeactivationByEmailResponse] shouldBe emailResponse
          status shouldBe StatusCodes.OK
        }
      }
      "return server error if user deactivation was failed" in {
        val deactivateUserByEmailRequest = "email"

        (userService.deactivateByEmail _ when deactivateUserByEmailRequest)
          .returns(Future.failed(new RuntimeException("Something wrong.")))

        Delete("/users/deactivate", deactivateUserByEmailRequest) ~> userController.route ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
      "return BadRequest status if user deactivation was failed" in {
        val deactivateUserByEmailRequest = "email"

        (userService.deactivateByEmail _ when deactivateUserByEmailRequest).returns(Future(None))

        Delete("/users/deactivate", deactivateUserByEmailRequest) ~> userController.route ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
    }
    "deactivateById" should {
      "return id and false value if user was successfully deactivated" in {
        val userId = Iterable.fill(36)("a").mkString
        val idResponse = UserDeactivationByIdResponse(UserId(userId), active = false)

        (userService.deactivateById _ when UserId(userId)).returns(Future(Option(idResponse)))

        Delete(s"/users/deactivate/$userId") ~> userController.route ~> check {
          responseAs[UserDeactivationByIdResponse] shouldBe idResponse
          status shouldBe StatusCodes.OK
        }
      }
      "return server error if user deactivation was failed" in {
        val userId = Iterable.fill(36)("a").mkString
        (userService.deactivateById _ when UserId(userId))
          .returns(Future.failed(new RuntimeException("Something wrong.")))

        Delete(s"/users/deactivate/$userId") ~> userController.route ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
      "return BabRequest status if user deactivation was failed" in {
        val userId = Iterable.fill(36)("a").mkString
        (userService.deactivateById _ when UserId(userId)).returns(Future(None))

        Delete(s"/users/deactivate/$userId") ~> userController.route ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
      "should return error when user's not found" in {
        val deactivateUserRequestByEmail = "email"
        (userService.deactivateByEmail _ when deactivateUserRequestByEmail).returns(Future(None))
        Delete("/users/deactivate", deactivateUserRequestByEmail) ~> userController.route ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
      "should return error when we're not signed in" in {
        val fakeAuthCfg = AuthConfig("123", JwtAlgorithm.HS384, ExpirationTimeInSeconds(1, 1, 1))
        val securityDirective = new SecurityDirective(fakeAuthCfg)
        val deactivateUserByEmailRequest = "email"
        (userService.deactivateByEmail _ when deactivateUserByEmailRequest).returns(Future(None))
        Delete("/users/deactivate", deactivateUserByEmailRequest) ~> securityDirective.authenticated { _ =>
          userController.route
        } ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }
      "should return err when id is not 36 symbols" in {
        val userId = "not-user-id"
        val idResponse = UserDeactivationByIdResponse(UserId(userId), active = false)
        (userService.deactivateById _ when UserId(userId)).returns(Future(Option(idResponse)))
        Delete(s"/users/deactivate/$userId") ~> userController.route ~> check {
          handled shouldBe false
        }
      }
    }
  }
}
