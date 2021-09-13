package cromwell.pipeline.service

import cats.implicits._
import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dao.utils.TestUserUtils
import cromwell.pipeline.datastorage.dto.User
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.model.validator.Enable
import cromwell.pipeline.model.wrapper.{ Name, Password, UserEmail, UserId }
import cromwell.pipeline.service.UserService.Exceptions.NotFound
import cromwell.pipeline.utils.StringUtils._
import org.mockito.Mockito._
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class UserServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {
  private val userRepository: UserRepository = mock[UserRepository]
  private val userService: UserService = UserService(userRepository)

  private val salt = "salt"
  private val password: Password = Password("Password_1", Enable.Unsafe)
  private val newPassword: Password = Password("newPassword_1", Enable.Unsafe)
  private val user = TestUserUtils.getDummyUserWithCredentials(password = password, passwordSalt = salt)

  "UserService" when {
    "deactivateUserById" should {

      "returns user's entity with false value" taggedAs Service in {
        when(userRepository.deactivateUserById(user.userId)).thenReturn(Future.successful(1))
        when(userRepository.getUserById(user.userId)).thenReturn(Future.successful(Some(user)))

        val response = User.fromUserWithCredentials(user)
        userService.deactivateUserById(user.userId).map { result =>
          result shouldBe response
        }
      }

      "return NotFound if user wasn't found by Id" taggedAs Service in {
        val userId = UserId.random

        when(userRepository.deactivateUserById(userId)).thenReturn(Future.successful(0))
        when(userRepository.getUserById(userId)).thenReturn(Future(None))

        userService.deactivateUserById(userId).failed.map { result =>
          result shouldBe NotFound()
        }
      }
    }

    "updateUser" should {

      "returns success if database handles query" in {
        val user2 = user.copy(
          email = UserEmail("updatedEmail@mail.com", Enable.Unsafe),
          firstName = Name("updatedFirstName", Enable.Unsafe),
          lastName = Name("updatedLastName", Enable.Unsafe)
        )
        val request = UserUpdateRequest(
          user2.email,
          user2.firstName,
          user2.lastName
        )

        when(userRepository.getUserById(user.userId)).thenReturn(Future.successful(Some(user)))
        when(userRepository.updateUser(user2)).thenReturn(Future.successful(1))

        userService.updateUser(user.userId, request).map { result =>
          result shouldBe 1
        }
      }
    }

    "updatePassword" should {

      "returns success if database handles query" in {
        val request = PasswordUpdateRequest(password, newPassword, newPassword)
        val newPasswordHash = calculatePasswordHash(newPassword, salt)

        when(userRepository.getUserById(user.userId)).thenReturn(Future.successful(Some(user)))
        when(userRepository.updatePassword(user.userId, newPasswordHash, salt)).thenReturn(Future.successful(1))

        userService.updatePassword(user.userId, request, salt).map { result =>
          result shouldBe 1
        }
      }

      "fail if user doesn't exist" in {
        val request = PasswordUpdateRequest(password, newPassword, newPassword)

        when(userRepository.getUserById(user.userId)).thenReturn(Future.successful(None))

        userService.updatePassword(user.userId, request, salt).failed.map { error =>
          error should have.message("User with this id doesn't exist")
        }
      }

      "fail if new password repeated incorrectly" in {
        val repeatedNewPassword = Password(newPassword.unwrap + "blah", Enable.Unsafe)
        val request = PasswordUpdateRequest(password, newPassword, repeatedNewPassword)

        userService.updatePassword(user.userId, request, salt).failed.map { error =>
          error should have.message("New password incorrectly duplicated")
        }
      }

      "fail if user current password doesn't match with request current password" in {
        val wrongPwd = Password(password.unwrap + "blah", Enable.Unsafe)
        val request = PasswordUpdateRequest(wrongPwd, newPassword, newPassword)

        when(userRepository.getUserById(user.userId)).thenReturn(Future.successful(Some(user)))

        userService.updatePassword(user.userId, request, salt).failed.map { error =>
          error should have.message("Users password differs from entered")
        }
      }
    }
  }
}
