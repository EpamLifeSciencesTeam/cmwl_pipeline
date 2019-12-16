package cromwell.pipeline.datastorage.dao.repository

import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.datastorage.dto.User
import cromwell.pipeline.utils.auth.TestUserUtils
import cromwell.pipeline.{ ApplicationComponents, TestContainersUtils }
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, Matchers }

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

  }

}
