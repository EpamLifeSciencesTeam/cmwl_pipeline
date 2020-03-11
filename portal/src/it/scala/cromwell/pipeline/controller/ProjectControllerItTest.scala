package cromwell.pipeline.controller

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import com.typesafe.config.Config
import cromwell.pipeline.ApplicationComponents
import cromwell.pipeline.datastorage.dao.repository.utils.{TestProjectUtils, TestUserUtils}
import cromwell.pipeline.datastorage.dto.{Project, ProjectDeleteRequest, ProjectId, ProjectUpdateRequest, UserId}
import cromwell.pipeline.datastorage.utils.auth.AccessTokenContent
import cromwell.pipeline.utils.TestContainersUtils
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import org.scalatest.{AsyncWordSpec, Matchers}

class ProjectControllerItTest
  extends AsyncWordSpec
    with Matchers
    with ScalatestRouteTest
    with ForAllTestContainer {

  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  container.start()
  implicit val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  private val components: ApplicationComponents = new ApplicationComponents()
  override protected def beforeAll(): Unit = components.datastorageModule.pipelineDatabaseEngine.updateSchema()
  private val ownerId = "1"
  private val dummyUser = TestUserUtils.getDummyUser(ownerId)
  private def projectId = ProjectId(UUID.randomUUID().toString)
  import components.controllerModule.projectController
  import components.datastorageModule.projectRepository
  import components.datastorageModule.userRepository

  "ProjectController" when {
    "getProjectByName" should {
      "return a project with the same name" in {
        val dummyProject = TestProjectUtils.getDummyProject(projectId, UserId(ownerId))
        userRepository
          .addUser(dummyUser)
          .flatMap(
            _ =>
              projectRepository.addProject(dummyProject).map { _ =>
                val accessToken = AccessTokenContent(dummyProject.ownerId.value)
                Get("/projects?name=" + dummyProject.name) ~> projectController.route(accessToken) ~> check {
                  status shouldBe StatusCodes.OK
                  responseAs[Option[Project]] shouldEqual Some(dummyProject)
                }
              }
          )
      }
    }
  }

  "updateProject" should {
    "return status code NoContend if project was successfully updated" in {
      val dummyProject = TestProjectUtils.getDummyProject(projectId, UserId(ownerId))
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
    "return project's entity with a field active equal to false if project was successfully deactivated" in {
      val dummyProject = TestProjectUtils.getDummyProject(projectId, UserId(ownerId))
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