package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.{ User, UserDeactivationResponse, UserId }
import org.mockito.Mockito._
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class UserServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {
  private val userRepository: UserRepository = mock[UserRepository]
  private val userService: UserService = new UserService(userRepository)

  "UserService" when {
    "deactivateByEmail" should {
      "return user's email and active value" in {
        val email = "email"
        val user = User(UserId("123"), email, "hash", "salt", "name", "lastName", active = false)

        when(userRepository.deactivateByEmail(email)).thenReturn(Future.successful(1))
        when(userRepository.getUserByEmail(email)).thenReturn(Future(Some(user)))

        val response = UserDeactivationResponse.fromUser(user)
        userService.deactivateByEmail(email).map { result =>
          result shouldBe Some(response)
        }
      }
      "return None if user wasn't found by email" in {
        val email = "email"
        when(userRepository.deactivateByEmail(email)).thenReturn(Future.successful(0))
        when(userRepository.getUserByEmail(email)).thenReturn(Future(None))

        userService.deactivateByEmail(email).map { result =>
          result shouldBe None
        }
      }
    }
    "deactivateById" should {
      "returns user's id and active value" in {
        val userId = UserId("123")
        val user = User(UserId("123"), "email", "hash", "salt", "name", "lastName", active = false)

        when(userRepository.deactivateById(userId)).thenReturn(Future.successful(1))
        when(userRepository.getUserById(userId)).thenReturn(Future(Some(user)))

        val response = UserDeactivationResponse.fromUser(user)
        userService.deactivateById(userId).map { result =>
          result shouldBe Some(response)
        }
      }
      "return None if user wasn't found by Id" in {
        val userId = UserId("123")

        when(userRepository.deactivateById(userId)).thenReturn(Future.successful(0))
        when(userRepository.getUserById(userId)).thenReturn(Future(None))

        userService.deactivateById(userId).map { result =>
          result shouldBe None
        }
      }
    }
  }
}
