package cromwell.pipeline.controller

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import com.typesafe.config.Config
import cromwell.pipeline.ApplicationComponents
import cromwell.pipeline.datastorage.dao.repository.utils.{TestProjectUtils, TestUserUtils}
import cromwell.pipeline.datastorage.dto.{Project, ProjectId, UserId}
import cromwell.pipeline.datastorage.utils.auth.AccessTokenContent
import cromwell.pipeline.utils.TestContainersUtils
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{AsyncWordSpec, Matchers}

class ProjectControllerItTest
  extends AsyncWordSpec
    with Matchers
    with ScalatestRouteTest
    with PlayJsonSupport
    with ForAllTestContainer {

  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  container.start()
  implicit val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  private val components: ApplicationComponents = new ApplicationComponents()
  override protected def beforeAll(): Unit = components.datastorageModule.pipelineDatabaseEngine.updateSchema()

  import components.controllerModule.projectController
  import components.datastorageModule.projectRepository
  import components.datastorageModule.userRepository

  "ProjectController" when {
    "getProjectByName" should {
      "return a project with the same name" in {
        val dummyUser = TestUserUtils.getDummyUser("1")
        val dummyProject = TestProjectUtils.getDummyProject(ProjectId(UUID.randomUUID().toString), UserId("1"))
        val projectByNameRequest = dummyProject.name

        userRepository
          .addUser(dummyUser)
          .flatMap(
            _ =>
              projectRepository.addProject(dummyProject).map { _ =>
                val accessToken = AccessTokenContent(dummyProject.ownerId.value)
                Get("/projects?name=" + projectByNameRequest) ~> projectController.route(accessToken) ~> check {
                  status shouldBe StatusCodes.OK
                  responseAs[Option[Project]] shouldEqual Option(dummyProject)
                }
              }
          )
      }
    }
  }
}
