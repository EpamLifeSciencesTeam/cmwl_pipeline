package cromwell.pipeline.datastorage.dao.repository

import java.util.UUID

import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.datastorage.dto.{ User, UserId }
import cromwell.pipeline.utils.StringUtils
import cromwell.pipeline.{ ApplicationComponents, TestContainersUtils }
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, Matchers }

class UserRepositoryTest extends AsyncWordSpec with Matchers with BeforeAndAfterAll with ForAllTestContainer {

  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  container.start()
  implicit val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  private val components: ApplicationComponents = new ApplicationComponents()

  override protected def beforeAll(): Unit =
    components.datastorageModule.pipelineDatabaseEngine.updateSchema()

  private val userPassword = "-Pa$$w0rd-"

  import components.datastorageModule.userRepository

  "UserRepository" when {

    "getUserById" should {

      "should find newly added user by id" in {
        val newUser = getDummyUser(userPassword)

        val addUserFuture = userRepository.addUser(newUser)
        val result = for {
          _ <- addUserFuture
          getById <- userRepository.getUserById(newUser.userId)
        } yield getById

        result.map(optUser => optUser shouldEqual Some(newUser))
      }
    }

    "getUserByEmail" should {

      "should find newly added user by email" in {
        val newUser = getDummyUser(userPassword)

        val addUserFuture = userRepository.addUser(newUser)
        val result = for {
          _ <- addUserFuture
          getByEmail <- userRepository.getUserByEmail(newUser.email)
        } yield getByEmail

        result.map(optUser => optUser shouldEqual Some(newUser))
      }
    }

  }

  private def getDummyUser(password: String = userPassword, passwordSalt: String = "salt"): User = {
    val uuid = UUID.randomUUID().toString
    val passwordHash = StringUtils.calculatePasswordHash(password, passwordSalt)
    User(
      userId = UserId(uuid),
      email = s"JohnDoe-$uuid@cromwell.com",
      passwordHash = passwordHash,
      passwordSalt = passwordSalt,
      firstName = "FirstName",
      lastName = "LastName",
      profilePicture = None
    )
  }
}
