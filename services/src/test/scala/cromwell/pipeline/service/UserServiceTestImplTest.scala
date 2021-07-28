package cromwell.pipeline.service

import cats.implicits._
import cromwell.pipeline.datastorage.dao.utils.TestUserUtils
import cromwell.pipeline.datastorage.dto.UserNoCredentials
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.model.validator.Enable
import cromwell.pipeline.model.wrapper.{ Name, Password, UserEmail }
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterEach, Matchers }

class UserServiceTestImplTest extends AsyncWordSpec with Matchers with BeforeAndAfterEach {

  private val userService: UserServiceTestImpl = UserServiceTestImpl()

  override protected def afterEach(): Unit = {
    super.afterEach()
    userService.clearTestRepository()
  }

  "UserService" when {
    "deactivateUserById" should {

      "returns user's entity with false value" taggedAs Service in {
        val user = TestUserUtils.getDummyUser()
        val noActiveUser = user.copy(active = false)
        val deactivatedUser = UserNoCredentials.fromUser(noActiveUser)

        userService.addDummies(user)
        userService.deactivateUserById(user.userId).map { result =>
          result shouldBe Some(deactivatedUser)
        }
      }
      "return None if user wasn't found by Id" taggedAs Service in {
        val userNotInRepository = TestUserUtils.getDummyUser()

        userService.deactivateUserById(userNotInRepository.userId).map { result =>
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
          user2.email,
          user2.firstName,
          user2.lastName
        )
        userService.addDummies(user)
        userService.updateUser(user.userId, request).map { result =>
          result shouldBe 1
        }
      }
    }

    "updatePassword" should {

      "returns success if database handles query" in {
        val user = TestUserUtils.getDummyUser()
        val salt = "salt"
        val validPassword: String = "newPassword_1"

        val request =
          PasswordUpdateRequest(
            TestUserUtils.userPassword,
            Password(validPassword, Enable.Unsafe),
            Password(validPassword, Enable.Unsafe)
          )
        userService.addDummies(user)

        userService.updatePassword(user.userId, request, salt).map { result =>
          result shouldBe 1
        }
      }
    }
  }

}
