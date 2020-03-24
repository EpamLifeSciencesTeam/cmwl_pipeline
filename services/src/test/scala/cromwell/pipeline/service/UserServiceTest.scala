package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dao.repository.utils.TestUserUtils
import cromwell.pipeline.datastorage.dto.formatters.UserFormatters.{PasswordUpdateRequest, UserNoCredentials, UserUpdateRequest}
import cromwell.pipeline.datastorage.dto.{User, UserId}
import cromwell.pipeline.utils.StringUtils._
import org.mockito.Mockito._
import org.scalatest.{AsyncWordSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class UserServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {
  private val userRepository: UserRepository = mock[UserRepository]
  private val userService: UserService = new UserService(userRepository)
  private val dummyUser: User = TestUserUtils.getDummyUser()
  private val userId: UserId = UserId("123")
  private lazy val userByEmailRequest: String = "@gmail"
  private lazy val userRepositoryResp = Seq(dummyUser)
  private lazy val userServiceResp: Seq[User] = Seq(dummyUser)

  "UserService" when {

    "invoke UserService" should {
      "get userResponse sequence from users sequence" taggedAs Service in {

        when(userRepository.getUsersByEmail(userByEmailRequest)).thenReturn(Future.successful(userRepositoryResp))
        userService.getUsersByEmail(userByEmailRequest).map { result =>
          result shouldBe userServiceResp
        }
      }
    }

    "deactivateUserById" should {
      "returns user's entity with false value" taggedAs Service in {
        val user: User = dummyUser.copy(userId = userId)

        when(userRepository.deactivateUserById(userId)).thenReturn(Future.successful(1))
        when(userRepository.getUserById(userId)).thenReturn(Future(Some(user)))

        val response = UserNoCredentials.fromUser(user)
        userService.deactivateUserById(userId).map { result =>
          result shouldBe Some(response)
        }
      }
      "return None if user wasn't found by Id" taggedAs Service in {
        when(userRepository.deactivateUserById(userId)).thenReturn(Future.successful(0))
        when(userRepository.getUserById(userId)).thenReturn(Future(None))

        userService.deactivateUserById(userId).map { result =>
          result shouldBe None
        }
      }
    }

    "updateUser" should {
      "returns success if database handles query" taggedAs Service in {
        val numberId = "123"
        val updatedUser =
          dummyUser.copy(email = "updatedEmail", firstName = "updatedFirstName", lastName = "updatedLastName")
        val request = UserUpdateRequest("updatedEmail", "updatedFirstName", "updatedLastName")

        when(userRepository.getUserById(userId)).thenReturn(Future(Some(dummyUser)))
        when(userRepository.updateUser(updatedUser)).thenReturn(Future.successful(1))

        userService.updateUser(numberId, request).map { result =>
          result shouldBe 1
        }
      }
    }

    "updatePassword" should {
      "returns success if database handles query" taggedAs Service in {
        val id = "123"
        val salt = "salt"
        val user =
          User(UserId(id), "email@cromwell.com", calculatePasswordHash("password", salt), salt, "name", "lastName")
        val request = PasswordUpdateRequest("password", "newPassword", "newPassword")
        val updatedUser =
          user.copy(passwordHash = calculatePasswordHash("newPassword", salt), passwordSalt = salt)

        when(userRepository.getUserById(UserId(id))).thenReturn(Future(Some(user)))
        when(userRepository.updatePassword(updatedUser)).thenReturn(Future.successful(1))

        userService.updatePassword(id, request, salt).map { result =>
          result shouldBe 1
        }
      }
    }
  }
}
