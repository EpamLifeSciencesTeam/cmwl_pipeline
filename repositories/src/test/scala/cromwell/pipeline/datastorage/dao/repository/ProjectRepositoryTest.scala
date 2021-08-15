package cromwell.pipeline.datastorage.dao.repository

import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.datastorage.DatastorageModule
import cromwell.pipeline.datastorage.dao.utils.{ PostgreTablesCleaner, TestProjectUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto.{ PipelineVersion, Project, UserWithCredentials }
import cromwell.pipeline.utils.{ ApplicationConfig, TestContainersUtils }
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, Matchers }

class ProjectRepositoryTest
    extends AsyncWordSpec
    with Matchers
    with BeforeAndAfterAll
    with ForAllTestContainer
    with PostgreTablesCleaner {

  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  protected lazy val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  protected lazy val datastorageModule: DatastorageModule = new DatastorageModule(ApplicationConfig.load(config))

  override protected def beforeAll(): Unit = {
    super.beforeAll
    datastorageModule.pipelineDatabaseEngine.updateSchema()
  }

  private val dummyUser: UserWithCredentials = TestUserUtils.getDummyUserWithCredentials()
  private val dummyProject: Project = TestProjectUtils.getDummyProject(ownerId = dummyUser.userId)

  import datastorageModule.{ projectRepository, userRepository }
  "ProjectRepository" when {

    "getUserById" should {

      "find newly added project by id" taggedAs Dao in {
        val addUserFuture = userRepository.addUser(dummyUser)
        val result = for {
          _ <- addUserFuture
          _ <- projectRepository.addProject(dummyProject)
          getById <- projectRepository.getProjectById(dummyProject.projectId)
        } yield getById
        result.map(optProject => optProject shouldEqual Some(dummyProject))
      }
    }

    "updateProjectVersion" should {

      "return project with correct version" taggedAs Dao in {
        val newVersion = PipelineVersion("v0.0.2")
        val result = for {
          _ <- userRepository.addUser(dummyUser)
          _ <- projectRepository.addProject(dummyProject)
          _ <- projectRepository.updateProjectVersion(dummyProject.copy(version = newVersion))
          newVersionProject <- projectRepository.getProjectById(dummyProject.projectId)
        } yield newVersionProject
        result.map(newVersionProject => newVersionProject shouldEqual Some(dummyProject.copy(version = newVersion)))
      }
    }
  }
}
