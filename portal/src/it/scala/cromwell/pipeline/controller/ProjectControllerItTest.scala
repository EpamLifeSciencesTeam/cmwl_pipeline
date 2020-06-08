package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.ApplicationComponents
import cromwell.pipeline.datastorage.dao.repository.utils.{ TestProjectUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto.{ Project, ProjectDeleteRequest, ProjectUpdateRequest }
import cromwell.pipeline.datastorage.formatters.ProjectFormatters._
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.utils.TestContainersUtils
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{ AsyncWordSpec, Matchers }

class ProjectControllerItTest
    extends AsyncWordSpec
    with Matchers
    with ScalatestRouteTest
    with PlayJsonSupport
    with ForAllTestContainer {

  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  implicit lazy val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  private lazy val components: ApplicationComponents = new ApplicationComponents()

  import components.controllerModule.projectController
  import components.datastorageModule.{ projectRepository, userRepository }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    components.datastorageModule.pipelineDatabaseEngine.updateSchema()
  }

  private val dummyUser = TestUserUtils.getDummyUser()

  "ProjectController" when {
    "getProjectByName" should {
      "return a project with the same name" in {
        val dummyProject =
          TestProjectUtils.getDummyProject(ownerId = dummyUser.userId)
        val projectByNameRequest = dummyProject.name

        userRepository
          .addUser(dummyUser)
          .flatMap(
            _ =>
              projectRepository.addProject(dummyProject).map { _ =>
                val accessToken = AccessTokenContent(dummyProject.ownerId)
                Get("/projects?name=" + projectByNameRequest) ~> projectController.route(accessToken) ~> check {
                  status shouldBe StatusCodes.OK
                  responseAs[Option[Project]] shouldEqual Option(dummyProject)
                }
              }
          )
      }
    }

    "updateProject" should {
      "return status code NoContend if project was successfully updated" in {
        val dummyProject =
          TestProjectUtils.getDummyProject(ownerId = dummyUser.userId)
        val request = ProjectUpdateRequest(dummyProject.projectId, dummyProject.name, dummyProject.repository)
        projectRepository
          .addProject(dummyProject)
          .flatMap(
            _ =>
              projectRepository.updateProject(dummyProject).map { _ =>
                val accessToken = AccessTokenContent(dummyUser.userId)
                Put("/projects", request) ~> projectController.route(accessToken) ~> check {
                  status shouldBe StatusCodes.NoContent
                }
              }
          )
      }
    }

    "deleteProjectById" should {
      "return project's entity with false value if project was successfully deactivated" in {
        val dummyProject =
          TestProjectUtils.getDummyProject(ownerId = dummyUser.userId)
        val request = ProjectDeleteRequest(dummyProject.projectId)
        val deactivatedProjectResponse = dummyProject.copy(active = false)
        projectRepository.addProject(dummyProject).map { _ =>
          val accessToken = AccessTokenContent(dummyUser.userId)
          Delete("/projects", request) ~> projectController.route(accessToken) ~> check {
            responseAs[Project] shouldBe deactivatedProjectResponse
          }
        }
      }
    }

  }
}
