package cromwell.pipeline.service

import cats.implicits._
import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.tag.Service
import cromwell.pipeline.utils.StringUtils._
import cromwell.pipeline.utils.auth.TestUserUtils
import org.mockito.Mockito._
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
      "returns user's entity with false value" taggedAs Service in {
        val userId = UUID("123e4567-e89b-12d3-a456-426655440000")
        val user = User(
          userId = UUID("123e4567-e89b-12d3-a456-426655440000"),
          email = UserEmail("someDomain@cromwell.com"),
          passwordHash = "hash",
          passwordSalt = "salt",
          firstName = Name("name"),
          lastName = Name("lastName")
        )

        when(userRepository.deactivateUserById(userId)).thenReturn(Future.successful(1))
        when(userRepository.getUserById(userId)).thenReturn(Future(Some(user)))

        val response = UserNoCredentials.fromUser(user)
        userService.deactivateUserById(userId).map { result =>
          result shouldBe Some(response)
        }
      }
      "return None if user wasn't found by Id" taggedAs Service in {
        val userId = UUID("123e4567-e89b-12d3-a456-426655440000")

        when(userRepository.deactivateUserById(userId)).thenReturn(Future.successful(0))
        when(userRepository.getUserById(userId)).thenReturn(Future(None))

        userService.deactivateUserById(userId).map { result =>
          result shouldBe None
        }
      }
    }

    "updateUser" should {
      "returns success if database handles query" in {
        val userId = UUID("123e4567-e89b-12d3-a456-426655440000")
        val user = User(
          userId = userId,
          email = UserEmail("someDomain@cromwell.com"),
          passwordHash = "hash",
          passwordSalt = "salt",
          firstName = Name("name"),
          lastName = Name("lastName"),
          active = false
        )
        val user2 = user.copy(
          email = UserEmail("updatedEmail@mail.com"),
          firstName = Name("updatedFirstName"),
          lastName = Name("updatedLastName")
        )
        val request = UserUpdateRequest(
          UserEmail("updatedEmail@mail.com"),
          Name("updatedFirstName"),
          Name("updatedLastName")
        )

        when(userRepository.getUserById(userId)).thenReturn(Future(Some(user)))
        when(userRepository.updateUser(user2)).thenReturn(Future.successful(1))

        userService.updateUser(userId, request).map { result =>
          result shouldBe 1
        }
      }
    }

    "updatePassword" should {
      "returns success if database handles query" in {
        val id = UUID.random
        val salt = "salt"
        val user =
          User(
            userId = id,
            email = UserEmail("email@cromwell.com"),
            calculatePasswordHash("password", salt),
            passwordSalt = salt,
            firstName = Name("name"),
            lastName = Name("lastName")
          )
        val request = PasswordUpdateRequest("password", "newPassword", "newPassword")
        val updatedUser1 = user.copy(passwordHash = calculatePasswordHash("newPassword", salt), passwordSalt = salt)

        when(userRepository.getUserById(id)).thenReturn(Future(Some(user)))
        when(userRepository.updatePassword(updatedUser1)).thenReturn(Future.successful(1))

        userService.updatePassword(id, request, salt).map { result =>
          result shouldBe 1
        }
      }
    }
  }
}
