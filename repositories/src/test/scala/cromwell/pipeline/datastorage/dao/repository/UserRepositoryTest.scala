package cromwell.pipeline.datastorage.dao.repository

import cats.implicits._
import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.datastorage.DatastorageModule
import cromwell.pipeline.datastorage.dao.utils.{ PostgreTablesCleaner, TestUserUtils }
import cromwell.pipeline.model.validator.Enable
import cromwell.pipeline.model.wrapper.{ Name, Password, UserEmail }
import cromwell.pipeline.utils.{ ApplicationConfig, StringUtils, TestContainersUtils }
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, Matchers }

class UserRepositoryTest
    extends AsyncWordSpec
    with Matchers
    with BeforeAndAfterAll
    with ForAllTestContainer
    with PostgreTablesCleaner {

  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  protected lazy val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  protected lazy val datastorageModule: DatastorageModule = new DatastorageModule(ApplicationConfig.load(config))
  import datastorageModule.userRepository

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    datastorageModule.pipelineDatabaseEngine.updateSchema()
  }

  private val newPassword: Password = Password("newPassword_1", Enable.Unsafe)
  private val newPasswordHash: String = StringUtils.calculatePasswordHash(newPassword, "salt")

  "UserRepository" when {

    "getUserById" should {

      "find newly added user by id" taggedAs Dao in {
        val dummyUser = TestUserUtils.getDummyUser()
        val result = for {
          _ <- userRepository.addUser(dummyUser)
          getById <- userRepository.getUserById(dummyUser.userId)
        } yield getById

        result.map(optUser => optUser shouldEqual Some(dummyUser))
      }
    }

    "getUserByEmail" should {

      "find newly added user by email" taggedAs Dao in {
        val dummyUser = TestUserUtils.getDummyUser()
        val result = for {
          _ <- userRepository.addUser(dummyUser)
          getByEmail <- userRepository.getUserByEmail(dummyUser.email)
        } yield getByEmail

        result.map(optUser => optUser shouldEqual Some(dummyUser))
      }
    }

    "getUsersByEmail" should {

      "should find newly added user by email pattern" taggedAs Dao in {
        val dummyUser = TestUserUtils.getDummyUser()
        userRepository.addUser(dummyUser).flatMap { _ =>
          userRepository.getUsersByEmail(dummyUser.email.unwrap).map(_ should contain theSameElementsAs Seq(dummyUser))
        }
      }
    }

    "updateUser" should {

      "update email, firstName and lastName" taggedAs Dao in {
        val dummyUser = TestUserUtils.getDummyUser()
        val updatedUser =
          dummyUser.copy(
            email = UserEmail("updated@email.com", Enable.Unsafe),
            firstName = Name("updatedFName", Enable.Unsafe),
            lastName = Name("updatedLName", Enable.Unsafe)
          )
        val result = for {
          _ <- userRepository.addUser(dummyUser)
          _ <- userRepository.updateUser(updatedUser)
          updatedDataUser <- userRepository.getUserById(dummyUser.userId)
        } yield updatedDataUser
        result.map(updatedDataUser => updatedDataUser.get shouldEqual updatedUser)
      }
    }

    "updatePassword" should {

      "update password" taggedAs Dao in {
        val dummyUser = TestUserUtils.getDummyUser()
        val updatedUser = dummyUser.copy(passwordHash = newPasswordHash)
        val result = for {
          _ <- userRepository.addUser(dummyUser)
          _ <- userRepository.updatePassword(updatedUser)
          userWithNewPassword <- userRepository.getUserById(dummyUser.userId)
        } yield userWithNewPassword
        result.map(userWithNewPassword => userWithNewPassword.get shouldEqual updatedUser)
      }
    }
  }
}
