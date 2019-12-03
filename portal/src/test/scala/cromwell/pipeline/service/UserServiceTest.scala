package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.{ User, UserId, UserNoCredentials }
import cromwell.pipeline.spec.ServiceSpec
import cromwell.pipeline.tag.Service
import cromwell.pipeline.utils.auth.{ TestUserUtils }
import org.mockito.Mockito._

import scala.concurrent.Future

class UserServiceTest extends ServiceSpec {

  private val userRepository: UserRepository = mock[UserRepository]
  private val user = TestUserUtils.getDummyUser()
  private val userService: UserService = new UserService(userRepository)
  private val userByEmailRequest: String = "@gmail"
  private val userRepositoryResp = Seq(user)
  private val userServiceResp: Seq[User] = Seq(user)

  "UserServiceTest" when {
    "invoke UserService" should {
      "get userResponse sequence from users sequence" taggedAs (Service) in {

        when(userRepository.getUsersByEmail(userByEmailRequest)).thenReturn(Future.successful(userRepositoryResp))
        userService.getUsersByEmail(userByEmailRequest).map { result =>
          result shouldBe userServiceResp
        }
      }
    }
    "deactivateUserById" should {
      "returns user's entity with false value" in {
        val userId = UserId("123")
        val user = User(UserId("123"), "email@cromwell.com", "hash", "salt", "name", "lastName", active = false) //TODO -check if test do its work

        when(userRepository.deactivateUserById(userId)).thenReturn(Future.successful(1))
        when(userRepository.getUserById(userId)).thenReturn(Future(Some(user)))

        val response = UserNoCredentials.fromUser(user)
        userService.deactivateUserById(userId).map { result =>
          result shouldBe Some(response)
        }
      }
      "return None if user wasn't found by Id" in {
        val userId = UserId("123")

        when(userRepository.deactivateUserById(userId)).thenReturn(Future.successful(0))
        when(userRepository.getUserById(userId)).thenReturn(Future(None))

        userService.deactivateUserById(userId).map { result =>
          result shouldBe None
        }
      }
    }
  }
}
