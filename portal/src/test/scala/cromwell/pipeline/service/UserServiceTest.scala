package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.datastorage.dto.{ User, UserId, UserNoCredentials }
import cromwell.pipeline.tag.Service
import cromwell.pipeline.utils.auth.{ TestUserUtils }
import cromwell.pipeline.utils.StringUtils._
import cromwell.pipeline.utils.auth.TestUserUtils
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class UserServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {
  private val userRepository: UserRepository = mock[UserRepository]
  private val user = TestUserUtils.getDummyUser()
  private val userService: UserService = new UserService(userRepository)
  private val userByEmailRequest: String = "@gmail"
  private val userRepositoryResp = Seq(user)
  private val userServiceResp: Seq[User] = Seq(user)

  "UserService" when {
    "deactivateUserById" should {
      "returns user's entity with false value" taggedAs (Service) in {
        val userId = UserId("123")
        val user = User(UserId("123"), "email@cromwell.com", "hash", "salt", "name", "lastName", active = false)

        when(userRepository.deactivateUserById(userId)).thenReturn(Future.successful(1))
        when(userRepository.getUserById(userId)).thenReturn(Future(Some(user)))

        val response = UserNoCredentials.fromUser(user)
        userService.deactivateUserById(userId).map { result =>
          result shouldBe Some(response)
        }
      }
      "return None if user wasn't found by Id" taggedAs (Service) in {
        val userId = UserId("123")

        when(userRepository.deactivateUserById(userId)).thenReturn(Future.successful(0))
        when(userRepository.getUserById(userId)).thenReturn(Future(None))

        userService.deactivateUserById(userId).map { result =>
          result shouldBe None
        }
      }
    }

    "updateUser" should {
      "returns success if database handles query" in {
        val userId = "123"
        val user = User(UserId("123"), "email@cromwell.com", "hash", "salt", "name", "lastName")
        val user2 = user.copy(email = "updatedEmail", firstName = "updatedFirstName", lastName = "updatedLastName")
        val request = UserUpdateRequest("updatedEmail", "updatedFirstName", "updatedLastName")

        when(userRepository.getUserById(UserId("123"))).thenReturn(Future(Some(user)))
        when(userRepository.updateUser(user2)).thenReturn(Future.successful(1))

        userService.updateUser(userId, request).map { result =>
          result shouldBe 1
        }
      }
    }

    "updatePassword" should {
      "returns success if database handles query" in {
        val id = "123"
        val salt = "salt"
        val user =
          User(UserId(id), "email@cromwell.com", calculatePasswordHash("password", salt), salt, "name", "lastName")
        val request = PasswordUpdateRequest("password", "newPassword", "newPassword")
        val updatedUser1 = user.copy(passwordHash = calculatePasswordHash("newPassword", salt), passwordSalt = salt)

        when(userRepository.getUserById(UserId(id))).thenReturn(Future(Some(user)))
        when(userRepository.updatePassword(updatedUser1)).thenReturn(Future.successful(1))

        userService.updatePassword(id, request, salt).map { result =>
          result shouldBe 1
        }
      }
    }
  }
}
