package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.{ User, UserDeactivationByEmailResponse, UserDeactivationByIdResponse, UserId }
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.{ Matchers, WordSpec }

import scala.concurrent.{ ExecutionContext, Future }

class UserServiceTest extends WordSpec with Matchers with MockFactory {
  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private val userRepository: UserRepository = stub[UserRepository]
  private val userService: UserService = new UserService(userRepository)

  "UserService" when {
    "deactivateByEmail" should {
      "return user's email and active value" in {
        val email = "email"
        val user = User(UserId("123"), email, "hash", "salt", "name", "lastName", active = false)

        (userRepository.deactivateByEmail _ when email).returns(Future(1))
        (userRepository.getUserByEmail _ when email).returns(Future(Option(user)))

        val emailResponse = UserDeactivationByEmailResponse(email, active = false)
        userService.deactivateByEmail(email).map { result =>
          result shouldBe Some(emailResponse)
        }
      }
      "return None if user wasn't found by email" in {
        val email = "email"
        (userRepository.deactivateByEmail _ when email).returns(Future(0))
        (userRepository.getUserByEmail _ when email).returns(Future(None))

        whenReady(userService.deactivateByEmail(email)) { result =>
          result shouldBe None
        }
      }
    }
    "deactivateById" should {
      "returns user's id and active value" in {
        val userId = UserId("123")
        val user = User(UserId("123"), "email", "hash", "salt", "name", "lastName", active = false)

        (userRepository.deactivateById _ when userId).returns(Future(1))
        (userRepository.getUserById _ when userId).returns((Future(Option(user))))

        val idResponse = UserDeactivationByIdResponse(userId, active = false)
        whenReady(userService.deactivateById(userId)) { result =>
          result shouldBe Some(idResponse)
        }
      }
      "return None if user wasn't found by Id" in {
        val userId = UserId("123")

        (userRepository.deactivateById _ when userId).returns(Future(0))
        (userRepository.getUserById _ when userId).returns(Future(None))

        whenReady(userService.deactivateById(userId)) { result =>
          result shouldBe None
        }
      }
    }
  }
}
