package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.ApplicationComponents
import cromwell.pipeline.datastorage.dao.utils.{ TestProjectUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.datastorage.dto.{ Project, ProjectUpdateNameRequest }
import cromwell.pipeline.utils.{ TestContainersUtils, TestTimeout }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{ AsyncWordSpec, Matchers }
import scala.concurrent.Await

class ProjectControllerItTest
    extends AsyncWordSpec
    with Matchers
    with ScalatestRouteTest
    with PlayJsonSupport
    with ForAllTestContainer
    with TestTimeout {

  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  implicit lazy val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  private lazy val components: ApplicationComponents = new ApplicationComponents()

  import components.controllerModule.projectController
  import components.datastorageModule.{ projectRepository, userRepository }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    components.datastorageModule.pipelineDatabaseEngine.updateSchema()
    Await.result(
      userRepository.addUser(stranger).flatMap { _ =>
        userRepository.addUser(dummyUser).flatMap { _ =>
          projectRepository.addProject(dummyProject).map(_ => ())
        }
      },
      timeoutAsDuration
    )
  }

  private val dummyUser = TestUserUtils.getDummyUserWithCredentials()
  private val stranger = TestUserUtils.getDummyUserWithCredentials()
  private val dummyProject = TestProjectUtils.getDummyProject(ownerId = dummyUser.userId)

  "ProjectController" when {
    "getProjects" should {
      "return list projects" in {
        val accessToken = AccessTokenContent(dummyProject.ownerId)

        Get("/projects") ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[List[Project]] shouldEqual List(dummyProject)
        }
      }

      "return empty list if user has no projects" in {
        val accessToken = AccessTokenContent(stranger.userId)

        Get("/projects") ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[List[Project]] shouldEqual List()
        }
      }
    }

    "getProjectByName" should {
      "return a project with the same name and current userId" in {
        val projectByNameRequest = dummyProject.name
        val accessToken = AccessTokenContent(dummyProject.ownerId)

        Get("/projects?name=" + projectByNameRequest) ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Option[Project]] shouldEqual Option(dummyProject)
        }
      }

      "return status code 403 if user isn`t project owner" in {
        val projectByNameRequest = dummyProject.name
        val accessToken = AccessTokenContent(stranger.userId)

        Get("/projects?name=" + projectByNameRequest) ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.Forbidden
        }
      }

      "return a status code 404 if project doesn't exist" in {
        val nonExistProject = TestProjectUtils.getDummyProject()
        val projectByNameRequest = nonExistProject.name
        val accessToken = AccessTokenContent(nonExistProject.ownerId)

        Get("/projects?name=" + projectByNameRequest) ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NotFound
        }
      }
    }

    "updateProjectName" should {

      /**
       * Ignored because we have no gitlab test environment and test is falling
       */
      "return status code OK if project was successfully updated" ignore {
        val request = ProjectUpdateNameRequest(dummyProject.name)
        val accessToken = AccessTokenContent(dummyUser.userId)

        Put(s"/projects/${dummyProject.projectId.value}", request) ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
        }
      }

      "return status code 500 if user doesn't have access to project" in {
        val request = ProjectUpdateNameRequest(dummyProject.name)
        val accessToken = AccessTokenContent(stranger.userId)

        Put(s"/projects/${dummyProject.projectId.value}", request) ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }

    "deleteProjectById" should {
      "return project's entity with false value if project was successfully deactivated" in {
        val deactivatedProjectResponse = dummyProject.copy(active = false)
        val accessToken = AccessTokenContent(dummyUser.userId)

        Delete(s"/projects/${dummyProject.projectId.value}") ~> projectController.route(accessToken) ~> check {
          responseAs[Project] shouldBe deactivatedProjectResponse
        }
      }
    }
  }
}
