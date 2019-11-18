package cromwell.pipeline.repository

import java.util.UUID

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ Container, ForAllTestContainer }
import cromwell.pipeline.datastorage.dto.{ Project, ProjectId }
import cromwell.pipeline.{ ApplicationComponents, BaseItTest }
import io.qala.datagen.RandomShortApi.{ alphanumeric, english }
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.{ Matchers, WordSpec }

class ProjectRepositoryTest extends WordSpec with Matchers with ScalatestRouteTest with ForAllTestContainer {

  private val components = new ApplicationComponents()

  import components.datastorageModule._

  override val container: Container = BaseItTest.getPostgreSQLContainer()

  override protected def beforeAll(): Unit =
    pipelineDatabaseEngine.updateSchema()

  "ProjectRepository" should {
    "addProject" in {
      val newProject = getValidRandomProject
      whenReady(projectRepository.addProject(newProject)) { _ =>
        val futureProject = projectRepository.getProjectById(newProject.projectId)
        futureProject.map { savedProject =>
          assert(savedProject.get == newProject)
        }
      }
    }
  }

  private def getValidRandomProject: Project = {
    val uuid = UUID.randomUUID().toString
    Project(
      projectId = ProjectId(uuid),
      name = english(64),
      description = alphanumeric(64),
      repository = alphanumeric(64)
    )
  }
}
