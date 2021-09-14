package cromwell.pipeline.service

import cats.implicits._
import cromwell.pipeline.datastorage.dao.repository.impls.UserRepositoryTestImpl
import cromwell.pipeline.datastorage.dao.utils.TestUserUtils
import cromwell.pipeline.datastorage.dto.User
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.model.validator.Enable
import cromwell.pipeline.model.wrapper.{ Name, Password, UserEmail, UserId }
import org.scalatest.{ AsyncWordSpec, Matchers }

class UserServiceTest extends AsyncWordSpec with Matchers {

  private val salt = "salt"
  private val password: Password = Password("Password_1", Enable.Unsafe)
  private val newPassword: Password = Password("newPassword_1", Enable.Unsafe)
  private val user = TestUserUtils.getDummyUserWithCredentials(password = password, passwordSalt = salt)

  private val userRepository: UserRepositoryTestImpl = UserRepositoryTestImpl(user)
  private val userService: UserService = UserService(userRepository)

  "UserService" when {
    "deactivateUserById" should {

      "return user's entity with false value" taggedAs Service in {
        val response = User.fromUserWithCredentials(user).copy(active = false)

        userService.deactivateUserById(user.userId).map { result =>
          result shouldBe Some(response)
        }
      }

      "return None if user wasn't found by Id" taggedAs Service in {
        val userId = UserId.random

        userService.deactivateUserById(userId).map { result =>
          result shouldBe None
        }
      }
    }

    "updateUser" should {

      "return success if database handles query" in {
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

        userService.updateUser(user.userId, request).map { result =>
          result shouldBe 1
        }
      }
    }

    "updatePassword" should {

      "return success if database handles query" in {
        val request = PasswordUpdateRequest(password, newPassword, newPassword)

        userService.updatePassword(user.userId, request, salt).map { result =>
          result shouldBe 1
        }
      }

      "fail if user doesn't exist" in {
        val request = PasswordUpdateRequest(password, newPassword, newPassword)

        val userRepository = UserRepositoryTestImpl()
        val userService = UserService(userRepository)

        userService.updatePassword(user.userId, request, salt).failed.map { error =>
          error should have.message("user with this id doesn't exist")
        }
      }

      "fail if new password repeated incorrectly" in {
        val repeatedNewPassword = Password(newPassword.unwrap + "blah", Enable.Unsafe)
        val request = PasswordUpdateRequest(password, newPassword, repeatedNewPassword)

        val userRepository = UserRepositoryTestImpl()
        val userService = UserService(userRepository)

        userService.updatePassword(user.userId, request, salt).failed.map { error =>
          error should have.message("new password incorrectly duplicated")
        }
      }

      "fail if user current password doesn't match with request current password" in {
        val wrongPwd = Password(password.unwrap + "blah", Enable.Unsafe)
        val request = PasswordUpdateRequest(wrongPwd, newPassword, newPassword)

        userService.updatePassword(user.userId, request, salt).failed.map { error =>
          error should have.message("user password differs from entered")
        }
      }
    }
  }
}
