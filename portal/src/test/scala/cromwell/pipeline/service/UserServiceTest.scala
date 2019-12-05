package cromwell.pipeline.service

import java.util.UUID

import akka.actor.FSM.Failure
import akka.parboiled2.RuleTrace.Fail
import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.datastorage.dto.{ User, UserId }
import cromwell.pipeline.utils.StringUtils
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Failed, Matchers, WordSpec }
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.{ JsError, JsResult }
import play.api.libs.json.JsResult.Exception

import scala.concurrent.{ ExecutionContext, Future, Promise }

class UserServiceTest extends WordSpec with Matchers with MockFactory with ScalaFutures {
  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private val userRepository = stub[UserRepository]
  private val userService = new UserService(userRepository)

  "UserServiceTest" when {

    "updateUser" should {

      "return counts of amended users in db" in {
        val updateRequest = UserUpdateRequest("JohnDoe@cromwell.com", "FirstName", "LastName")
        val updatedUser =
          User(UserId("1"), email = "JohnDoe@cromwell.com", firstName = "FirstName", lastName = "LastName")
        (userRepository.updateUser _ when updatedUser).returns(Future(1))

        whenReady(userService.updateUser("1", updateRequest)) { result =>
          result shouldBe 1
        }
      }

      "fails if passwords don't match" in {
        val passwordUpdateRequest = PasswordUpdateRequest("Password1234", "Password12345")
        val result: Future[Int] = userService.updateUserPassword("1", passwordUpdateRequest)
        whenReady(result.failed) { e =>
          e shouldBe an[Exception]
        }
      }
    }
  }
}
