package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import com.typesafe.config.Config
import cromwell.pipeline.ApplicationComponents
import cromwell.pipeline.datastorage.dto.Project
import cromwell.pipeline.datastorage.dto.project.{ProjectDeleteRequest, ProjectUpdateRequest}
import cromwell.pipeline.utils.auth.{AccessTokenContent, TestContainersUtils, TestProjectUtils, TestUserUtils}
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
  val ownerId = "1"

  import components.controllerModule.projectController
  import components.datastorageModule.projectRepository
  import components.datastorageModule.userRepository

  "ProjectController" when {
    "getProjectByName" should {
      "return a project with the same name" in {
        val dummyUser = TestUserUtils.getDummyUser(ownerId)
        val dummyProject = TestProjectUtils.getDummyProject("11", ownerId)
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

  "updateProject" should {
    "return status code NoContend if project was successfully updated" in {
      val dummyUser = TestUserUtils.getDummyUser(ownerId)
      val dummyProject = TestProjectUtils.getDummyProject("22",ownerId)
      val request = ProjectUpdateRequest(dummyProject.projectId, dummyProject.name, dummyProject.repository)
      projectRepository
        .addProject(dummyProject)
        .flatMap( _ =>
        projectRepository.updateProject(dummyProject).map{ _ =>
          val accessToken = AccessTokenContent(dummyUser.userId.value)
          Put("/projects", request) ~> projectController.route(accessToken) ~> check {
            status shouldBe StatusCodes.NoContent
          }
        })
    }
  }

  "deleteProjectById" should {
    "return project's entity with false value if project was successfully deactivated" in {
      val dummyUser = TestUserUtils.getDummyUser(ownerId)
      val dummyProject = TestProjectUtils.getDummyProject("33",ownerId)
      val request = ProjectDeleteRequest(dummyProject.projectId)
      val deactivatedProjectResponse = dummyProject.copy(active = false)
      projectRepository.addProject(dummyProject).map{ _ =>
        val accessToken = AccessTokenContent(dummyUser.userId.value)
        Delete("/projects", request) ~> projectController.route(accessToken) ~> check {
          responseAs[Project] shouldBe deactivatedProjectResponse
        }
      }
    }
  }

}
