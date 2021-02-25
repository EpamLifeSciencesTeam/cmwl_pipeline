package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dao.utils.{ TestProjectUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.datastorage.dto.{ Project, ProjectDeleteRequest, ProjectUpdateNameRequest }
import cromwell.pipeline.service.ProjectService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class ProjectControllerTest extends AsyncWordSpec with Matchers with ScalatestRouteTest with MockitoSugar {

  private val projectService = mock[ProjectService]
  private val projectController = new ProjectController(projectService)

  "Project controller" when {
    "get project by name" should {
      "return a object of project type" taggedAs Controller in {
        val projectByName: String = "dummyProject"
        val dummyProject: Project = TestProjectUtils.getDummyProject()
        val getProjectByNameResponse: Project = dummyProject

        val accessToken = AccessTokenContent(dummyProject.ownerId)
        when(projectService.getUserProjectByName(projectByName, accessToken.userId))
          .thenReturn(Future.successful(getProjectByNameResponse))

        Get("/projects?name=" + projectByName) ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Project] shouldEqual getProjectByNameResponse
        }
      }
    }

    "delete project by id" should {
      "return deactivated project entity" taggedAs Controller in {
        val dummyProject = TestProjectUtils.getDummyProject()
        val accessToken = AccessTokenContent(TestUserUtils.getDummyUserId)
        val deactivatedProject = dummyProject.copy(active = false)
        val request = ProjectDeleteRequest(dummyProject.projectId)

        when(
          projectService.deactivateProjectById(dummyProject.projectId, accessToken.userId)
        ).thenReturn(Future.successful(deactivatedProject))

        Delete("/projects", request) ~> projectController.route(accessToken) ~> check {
          responseAs[Project] shouldBe deactivatedProject
        }

      }

      "return server error if project deactivation was failed" taggedAs Controller in {
        val userId = TestUserUtils.getDummyUserId
        val request = ProjectDeleteRequest(TestProjectUtils.getDummyProject().projectId)
        val accessToken = AccessTokenContent(userId)

        when(projectService.deactivateProjectById(request.projectId, userId))
          .thenReturn(Future.failed(new RuntimeException("Something wrong")))

        Delete("/projects", request) ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }

    "update project" should {
      "return status code NoContent if the update was successful" taggedAs Controller in {
        val userId = TestUserUtils.getDummyUserId
        val accessToken = AccessTokenContent(userId)
        val dummyProject = TestProjectUtils.getDummyProject()
        val request = ProjectUpdateNameRequest(dummyProject.projectId, dummyProject.name)

        when(projectService.updateProjectName(request, userId)).thenReturn(Future.successful(1))

        Put("/projects", request) ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NoContent
        }
      }

      "return InternalServerError status if the update failed" taggedAs Controller in {
        val userId = TestUserUtils.getDummyUserId
        val accessToken = AccessTokenContent(userId)
        val dummyProject = TestProjectUtils.getDummyProject()
        val request = ProjectUpdateNameRequest(dummyProject.projectId, dummyProject.name)

        when(projectService.updateProjectName(request, userId))
          .thenReturn(Future.failed(new RuntimeException("Something wrong")))

        Put("/projects", request) ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }
  }
}
