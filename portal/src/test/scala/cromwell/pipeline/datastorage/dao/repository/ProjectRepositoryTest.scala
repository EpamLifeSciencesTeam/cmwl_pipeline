package cromwell.pipeline.datastorage.dao.repository

import cats.implicits._
import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.ApplicationComponents
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.utils.StringUtils
import cromwell.pipeline.utils.auth.TestContainersUtils
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, Matchers }

class ProjectRepositoryTest extends AsyncWordSpec with Matchers with BeforeAndAfterAll with ForAllTestContainer {

  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  container.start()
  implicit val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  private val components: ApplicationComponents = new ApplicationComponents()

  override protected def beforeAll(): Unit =
    components.datastorageModule.pipelineDatabaseEngine.updateSchema()

  private val userPassword = "-Pa$$w0rd-"

  import components.datastorageModule.{ projectRepository, userRepository }

  "ProjectRepository" when {

    "getUserById" should {

      "find newly added project by id" in {
        val newUser = getDummyUser(userPassword)
        val newProject = getDummyProject(newUser.userId)

        val addUserFuture = userRepository.addUser(newUser)
        val result = for {
          _ <- addUserFuture
          _ <- projectRepository.addProject(newProject)
          getById <- projectRepository.getProjectById(newProject.projectId)
        } yield getById

        result.map(optProject => optProject shouldEqual Some(newProject))
      }
    }
  }

  private def getDummyUser(password: String = userPassword, passwordSalt: String = "salt"): User = {
    val uuid = UUID.random
    val passwordHash = StringUtils.calculatePasswordHash(password, passwordSalt)
    User(
      userId = UUID.random,
      email = UserEmail(s"JohnDoe-${uuid.unwrap}@cromwell.com"),
      passwordHash = passwordHash,
      passwordSalt = passwordSalt,
      firstName = Name("FirstName"),
      lastName = Name("LastName"),
      profilePicture = None
    )
  }

  private def getDummyProject(ownerId: UUID): Project = {
    val uuid = UUID.random
    Project(
      projectId = ProjectId(uuid),
      ownerId = ownerId,
      name = s"project-$uuid",
      repository = s"repo-$uuid",
      active = true
    )
  }
}
