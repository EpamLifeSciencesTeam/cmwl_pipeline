package cromwell.pipeline.controller

import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.{ HttpEntity, StatusCodes }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dto.user.UserUpdateRequest
import cromwell.pipeline.datastorage.dto.user.PasswordUpdateRequest
import cromwell.pipeline.service.UserService
import org.scalatest.{ Matchers, WordSpec }
import org.scalamock.scalatest.MockFactory
import play.api.libs.json.Json

import scala.concurrent.Future

class UserControllerTest extends WordSpec with Matchers with ScalatestRouteTest with MockFactory {

  private val userService = stub[UserService]
  private val userController = new UserController(userService)

  "UserController" when {
    "update user" should {
      "return status NoContent" in {
        val updateRequest = UserUpdateRequest("JohnDoe@cromwell.com", "FirstName", "LastName")
        val userId = "213"
        val httpEntity = HttpEntity(`application/json`, Json.stringify(Json.toJson(updateRequest)))
        (userService.updateUser _ when (userId, updateRequest)).returns(Future(1))

        Put("/users/" + userId, httpEntity) ~> userController.route ~> check {
          status shouldBe StatusCodes.NoContent
        }
      }
    }

    "update password" should {
      "return status NoContent" in {
        val updateRequest = PasswordUpdateRequest("Password214", "Password214")
        val userId = "214"
        val httpEntity = HttpEntity(`application/json`, Json.stringify(Json.toJson(updateRequest)))
        if (updateRequest.newPassword == updateRequest.repeatPassword)
          (userService.updateUserPassword _ when (userId, updateRequest)).returns(Future(1))
        else fail

        Put("/users/" + userId, httpEntity) ~> userController.route ~> check {
          status shouldBe StatusCodes.NoContent
        }
      }
    }
  }
}
