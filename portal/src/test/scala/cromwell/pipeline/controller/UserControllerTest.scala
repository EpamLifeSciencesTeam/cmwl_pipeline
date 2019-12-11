package cromwell.pipeline.controller

import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.{ HttpEntity, StatusCodes }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dto.user.DeactivateUserRequestByEmail
import cromwell.pipeline.datastorage.dto.{ UserDeactivationByEmailResponse, UserDeactivationByIdResponse, UserId }
import cromwell.pipeline.service.UserService
import cromwell.pipeline.utils.auth.SecurityDirective
import cromwell.pipeline.{ AuthConfig, ExpirationTimeInSeconds }
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Matchers, WordSpec }
import pdi.jwt.JwtAlgorithm
import play.api.libs.json.Json

import scala.concurrent.Future

class UserControllerTest extends WordSpec with Matchers with MockFactory with ScalatestRouteTest {

  private val UserService = stub[UserService]
  private val UserController = new UserController(UserService)

  "UserController" when {
    "deactivateByEmail" should {
      "return email and false value if user was successfully deactivated" in {
        val deactivateUserRequestByEmail = DeactivateUserRequestByEmail("email")
        val emailResponse = UserDeactivationByEmailResponse(email = "JohnDoe@cromwell.com", active = false)

        val httpEntity = HttpEntity(`application/json`, Json.stringify(Json.toJson(deactivateUserRequestByEmail)))
        (UserService.deactivateByEmail _ when deactivateUserRequestByEmail).returns(Future(Option(emailResponse)))

        Delete("/users/deactivate", httpEntity) ~> UserController.route ~> check {
          val response = Json.parse(responseAs[String]).as[UserDeactivationByEmailResponse]
          response shouldBe emailResponse
          status shouldBe StatusCodes.OK
        }
      }
      "return BadRequest status if user deactivation was failed" in {
        val deactivateUserRequestByEmail = DeactivateUserRequestByEmail("email")
        val httpEntity = HttpEntity(`application/json`, Json.stringify(Json.toJson(deactivateUserRequestByEmail)))
        (UserService.deactivateByEmail _ when deactivateUserRequestByEmail)
          .returns(Future(throw new RuntimeException("Something wrong.")))

        Delete("/users/deactivate", httpEntity) ~> UserController.route ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
    }
    "deactivateById" should {
      "return id and false value if user was successfully deactivated" in {
        val userId = Iterable.fill(36)("a").mkString
        val idResponse = UserDeactivationByIdResponse(UserId(userId), active = false)

        (UserService.deactivateById _ when UserId(userId)).returns(Future(Option(idResponse)))

        Delete(s"/users/deactivate/$userId") ~> UserController.route ~> check {
          val response = Json.parse(responseAs[String]).as[UserDeactivationByIdResponse]
          response shouldBe idResponse
          status shouldBe StatusCodes.OK
        }
      }
      "return BadRequest status if user deactivation was failed" in {
        val userId = Iterable.fill(36)("a").mkString
        (UserService.deactivateById _ when UserId(userId))
          .returns(Future(throw new RuntimeException("Something wrong.")))

        Delete(s"/users/deactivate/$userId") ~> UserController.route ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
      "should return error when user not found" in {
        val deactivateUserRequestByEmail = DeactivateUserRequestByEmail("email")
        val httpEntity = HttpEntity(`application/json`, Json.stringify(Json.toJson(deactivateUserRequestByEmail)))
        (UserService.deactivateByEmail _ when deactivateUserRequestByEmail).returns(Future(None))
        Delete("/users/deactivate", httpEntity) ~> UserController.route ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
      "should return error when we're not signed in" in {
        val fakeAuthCfg = AuthConfig("123", JwtAlgorithm.HS384, ExpirationTimeInSeconds(1, 1, 1))
        val securityDirective = new SecurityDirective(fakeAuthCfg)
        val deactivateUserRequestByEmail = DeactivateUserRequestByEmail("email")
        val httpEntity = HttpEntity(`application/json`, Json.stringify(Json.toJson(deactivateUserRequestByEmail)))
        (UserService.deactivateByEmail _ when deactivateUserRequestByEmail).returns(Future(None))
        Delete("/users/deactivate", httpEntity) ~> securityDirective.authenticated { _ =>
          UserController.route
        } ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }
      "should return err when id is not 36 symbols" in {
        val userId = "not-user-id"
        val idResponse = UserDeactivationByIdResponse(UserId(userId), active = false)
        (UserService.deactivateById _ when UserId(userId)).returns(Future(Option(idResponse)))
        Delete(s"/users/deactivate/$userId") ~> UserController.route ~> check {
          handled shouldBe false
        }
      }
    }
  }
}
