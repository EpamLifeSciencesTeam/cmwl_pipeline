package cromwell.pipeline.datastorage.dao.repository

import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.datastorage.dto.{ Name, UUID, User, UserEmail }
import cromwell.pipeline.utils.auth.{ TestContainersUtils, TestUserUtils }
import cromwell.pipeline.ApplicationComponents
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, Matchers }
import cromwell.pipeline.tag.Dao
import cats.implicits._

class UserRepositoryTest extends AsyncWordSpec with Matchers with BeforeAndAfterAll with ForAllTestContainer {

  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  container.start()
  implicit val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  private val components: ApplicationComponents = new ApplicationComponents()

  override protected def beforeAll(): Unit =
    components.datastorageModule.pipelineDatabaseEngine.updateSchema()

  import components.datastorageModule.userRepository

  "UserRepository" when {

    "getUserById" should {

      "find newly added user by id" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val addUserFuture = userRepository.addUser(dummyUser)
        val result = for {
          _ <- addUserFuture
          getById <- userRepository.getUserById(dummyUser.userId)
        } yield getById

        result.map(optUser => optUser shouldEqual Some(dummyUser))
      }
    }

    "getUserByEmail" should {

      "find newly added user by email" in {
        val dummyUser: User = TestUserUtils.getDummyUser()

        val addUserFuture = userRepository.addUser(dummyUser)
        val result = for {
          _ <- addUserFuture
          getByEmail <- userRepository.getUserByEmail(dummyUser.email)
        } yield getByEmail

        result.map(optUser => optUser shouldEqual Some(dummyUser))
      }
    }

    "getUsersByEmail" should {

      "should find newly added user by email pattern" taggedAs Dao in {
        val newUser: User = TestUserUtils.getDummyUser()
        userRepository
          .addUser(newUser)
          .flatMap(
            _ =>
              userRepository
                .getUsersByEmail(newUser.email)
                .map(repoResp => repoResp should contain theSameElementsAs Seq(newUser))
          )
      }
    }

    "updateUser" should {
      "update email, firstName and lastName" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        userRepository.addUser(dummyUser)

        val updatedUser =
          dummyUser.copy(
            email = UserEmail("updated@email.com"),
            firstName = Name("updatedFName"),
            lastName = Name("updatedLName")
          )
        userRepository
          .updateUser(updatedUser)
          .flatMap(
            _ => userRepository.getUserById(dummyUser.userId).map(dummyUser => dummyUser.get shouldEqual updatedUser)
          )
      }
    }

    "updatePassword" should {
      "update password" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        userRepository.addUser(dummyUser)

        val updatedUser = dummyUser.copy(passwordHash = TestUserUtils.getDummyUser(UUID.random).passwordHash)
        userRepository
          .updatePassword(updatedUser)
          .flatMap(
            _ => userRepository.getUserById(dummyUser.userId).map(dummyUser => dummyUser.get shouldEqual updatedUser)
          )
      }
    }
  }
}
