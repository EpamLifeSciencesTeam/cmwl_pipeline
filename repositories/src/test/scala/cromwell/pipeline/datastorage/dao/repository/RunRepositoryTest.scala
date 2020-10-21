package cromwell.pipeline.datastorage.dao.repository

import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.datastorage.DatastorageModule
import cromwell.pipeline.datastorage.dao.repository.utils.{ TestProjectUtils, TestRunUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto.{ Done, Project, Run, User }
import cromwell.pipeline.utils.{ ApplicationConfig, TestContainersUtils }
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, Matchers }

class RunRepositoryTest extends AsyncWordSpec with Matchers with BeforeAndAfterAll with ForAllTestContainer {

  private lazy val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  private lazy val datastorageModule: DatastorageModule = new DatastorageModule(ApplicationConfig.load(config))
  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    datastorageModule.pipelineDatabaseEngine.updateSchema()
  }

  import datastorageModule.{ projectRepository, runRepository, userRepository }
  "RunRepository" when {

    "getRunById" should {

      "find newly added run by id" taggedAs Dao in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val dummyProject: Project = TestProjectUtils.getDummyProject(ownerId = dummyUser.userId)
        val dummyRun: Run = TestRunUtils.getDummyRun(userId = dummyUser.userId, projectId = dummyProject.projectId)
        val addUserFuture = userRepository.addUser(dummyUser)
        val result = for {
          _ <- addUserFuture
          _ <- projectRepository.addProject(dummyProject)
          _ <- runRepository.addRun(dummyRun)
          getById <- runRepository.getRunById(dummyRun.runId)
        } yield getById
        result.map(optRun => optRun shouldEqual Some(dummyRun))
      }
    }

    "update run by id" should {

      "update status, time start, time end, result and cmwlWorkflowId" taggedAs Dao in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val dummyProject: Project = TestProjectUtils.getDummyProject(ownerId = dummyUser.userId)
        val dummyRun: Run = TestRunUtils.getDummyRun(userId = dummyUser.userId, projectId = dummyProject.projectId)
        val addUserFuture = userRepository.addUser(dummyUser)
        val updatedRun =
          dummyRun.copy(
            status = Done,
            timeStart = TestRunUtils.getDummyTimeStart,
            timeEnd = TestRunUtils.getDummyTimeEnd(false),
            results = "new-result",
            cmwlWorkflowId = TestRunUtils.getDummyCmwlWorkflowId(false)
          )

        val result = for {
          _ <- addUserFuture
          _ <- projectRepository.addProject(dummyProject)
          _ <- runRepository.addRun(dummyRun)
          _ <- runRepository.updateRun(updatedRun)
          getById <- runRepository.getRunById(dummyRun.runId)
        } yield getById

        result.map(optRun => optRun shouldEqual Some(updatedRun))
      }
    }

    "delete run" should {

      "delete run by id" taggedAs Dao in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val dummyProject: Project = TestProjectUtils.getDummyProject(ownerId = dummyUser.userId)
        val dummyRun: Run = TestRunUtils.getDummyRun(userId = dummyUser.userId, projectId = dummyProject.projectId)
        val addUserFuture = userRepository.addUser(dummyUser)
        val result = for {
          _ <- addUserFuture
          _ <- projectRepository.addProject(dummyProject)
          _ <- runRepository.addRun(dummyRun)
          _ <- runRepository.deleteRunById(dummyRun.runId)
          getById <- runRepository.getRunById(dummyRun.runId)
        } yield getById

        result.map(optRun => optRun shouldEqual None)
      }
    }
  }

}
