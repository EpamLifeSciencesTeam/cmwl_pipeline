package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dao.repository.utils.TestUserUtils
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.datastorage.dto.UserNoCredentials
import cromwell.pipeline.utils.StringUtils._
import org.mockito.Mockito._
import cats.implicits._
import cromwell.pipeline.model.validator.Enable
import cromwell.pipeline.model.wrapper.{ Name, UserEmail, UserId }
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class UserServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {
  private val userRepository: UserRepository = mock[UserRepository]
  private val userService: UserService = new UserService(userRepository)
  private val validPassword: String = "newPassword_1"

  "UserService" when {
    "deactivateUserById" should {

      "returns user's entity with false value" taggedAs Service in {
        val user = TestUserUtils.getDummyUser()

        when(userRepository.deactivateUserById(user.userId)).thenReturn(Future.successful(1))
        when(userRepository.getUserById(user.userId)).thenReturn(Future.successful(Some(user)))

        val response = UserNoCredentials.fromUser(user)
        userService.deactivateUserById(user.userId).map { result =>
          result shouldBe Some(response)
        }
      }

      "return None if user wasn't found by Id" taggedAs Service in {
        val userId = UserId.random

        when(userRepository.deactivateUserById(userId)).thenReturn(Future.successful(0))
        when(userRepository.getUserById(userId)).thenReturn(Future(None))

        userService.deactivateUserById(userId).map { result =>
          result shouldBe None
        }
      }
    }

    "updateUser" should {

      "returns success if database handles query" in {
        val user = TestUserUtils.getDummyUser()
        val user2 = user.copy(
          email = UserEmail("updatedEmail@mail.com", Enable.Unsafe),
          firstName = Name("updatedFirstName", Enable.Unsafe),
          lastName = Name("updatedLastName", Enable.Unsafe)
        )
        val request = UserUpdateRequest(
          user2.email.unwrap,
          user2.firstName.unwrap,
          user2.lastName.unwrap
        )

        when(userRepository.getUserById(user.userId)).thenReturn(Future.successful(Some(user)))
        when(userRepository.updateUser(user2)).thenReturn(Future.successful(1))

        userService.updateUser(user.userId, request).map { result =>
          result shouldBe 1
        }
      }

      "returns failed if user doesn't exist" in {
        val user = TestUserUtils.getDummyUser()
        val request = UserUpdateRequest(
          user.email.unwrap,
          user.firstName.unwrap,
          user.lastName.unwrap
        )

        when(userRepository.getUserById(user.userId)).thenReturn(Future.successful(None))

        userService.updateUser(user.userId, request).failed.map {
          _ should have.message("user has not been found")
        }
      }

      "returns failed if invalid field (email)" in {
        val user = TestUserUtils.getDummyUser()
        val request = UserUpdateRequest("", "", "")

        when(userRepository.getUserById(user.userId)).thenReturn(Future.successful(Some(user)))

        userService.updateUser(user.userId, request).failed.map {
          _ should have.message("NonEmptyChain(Email should match the following pattern <text_1>@<text_2>.<text_3>)")
        }
      }
    }

    "updatePassword" should {

      "returns success if database handles query" in {
        val user = TestUserUtils.getDummyUser()
        val salt = "salt"

        val request = PasswordUpdateRequest(TestUserUtils.userPassword.value, validPassword, validPassword)
        val updatedUser = user.copy(passwordHash = calculatePasswordHash(validPassword, salt), passwordSalt = salt)

        when(userRepository.getUserById(user.userId)).thenReturn(Future.successful(Some(user)))
        when(userRepository.updatePassword(updatedUser)).thenReturn(Future.successful(1))

        userService.updatePassword(user.userId, request, salt).map { result =>
          result shouldBe 1
        }
      }

      "returns failed if user doesn't exist" in {
        val user = TestUserUtils.getDummyUser()
        val salt = "salt"
        val request = PasswordUpdateRequest(TestUserUtils.userPassword.value, validPassword, validPassword)

        when(userRepository.getUserById(user.userId)).thenReturn(Future.successful(None))

        userService.updatePassword(user.userId, request, salt).failed.map {
          _ should have.message("user has not been found")
        }
      }

      "returns failed if invalid field (currentPassword)" in {
        val user = TestUserUtils.getDummyUser()
        val salt = "salt"
        val request = PasswordUpdateRequest("", "", "")

        when(userRepository.getUserById(user.userId)).thenReturn(Future.successful(Some(user)))

        userService.updatePassword(user.userId, request, salt).failed.map {
          _ should have.message(
            "NonEmptyChain(Password must be at least 10 characters long, " +
              "including an uppercase and a lowercase letter, " +
              "one number and one special character.)"
          )
        }
      }

      "returns failed if user password differs from entered" in {
        val user = TestUserUtils.getDummyUser()
        val salt = "salt"
        val request = PasswordUpdateRequest("Password_1", validPassword, validPassword)

        when(userRepository.getUserById(user.userId)).thenReturn(Future.successful(Some(user)))

        userService.updatePassword(user.userId, request, salt).failed.map {
          _ should have.message("user password differs from entered")
        }
      }

      "returns failed if new password incorrectly duplicated" in {
        val user = TestUserUtils.getDummyUser()
        val salt = "salt"
        val request = PasswordUpdateRequest(TestUserUtils.userPassword.value, validPassword, "Password_1")

        when(userRepository.getUserById(user.userId)).thenReturn(Future.successful(Some(user)))

        userService.updatePassword(user.userId, request, salt).failed.map {
          _ should have.message("new password incorrectly duplicated")
        }
      }
    }
  }
}
