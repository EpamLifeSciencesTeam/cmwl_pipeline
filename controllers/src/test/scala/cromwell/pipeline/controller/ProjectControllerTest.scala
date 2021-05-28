package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dao.utils.{ TestProjectUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.datastorage.dto.{
  Project,
  ProjectAdditionRequest,
  ProjectDeleteRequest,
  ProjectUpdateNameRequest
}
import cromwell.pipeline.service.{ ProjectService, VersioningException }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class ProjectControllerTest extends AsyncWordSpec with Matchers with ScalatestRouteTest with MockitoSugar {

  private val projectService = mock[ProjectService]
  private val projectController = new ProjectController(projectService)
  private val stranger = TestUserUtils.getDummyUser()

  "Project controller" when {
    "get project by name" should {
      val projectByName: String = "dummyProject"
      val dummyProject: Project = TestProjectUtils.getDummyProject()

      "return a object of project type" taggedAs Controller in {
        val getProjectByNameResponse: Project = dummyProject
        val accessToken = AccessTokenContent(dummyProject.ownerId)
        when(projectService.getUserProjectByName(projectByName, accessToken.userId))
          .thenReturn(Future.successful(getProjectByNameResponse))

        Get("/projects?name=" + projectByName) ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Project] shouldEqual getProjectByNameResponse
        }
      }

      "return status code InternalServerError if service returned failure" taggedAs Controller in {
        val accessToken = AccessTokenContent(stranger.userId)
        val error = new RuntimeException("Something went wrong")
        when(projectService.getUserProjectByName(projectByName, accessToken.userId)).thenReturn(Future.failed(error))

        Get("/projects?name=" + projectByName) ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }

    "add project" should {

      val dummyProject = TestProjectUtils.getDummyProject()
      val accessToken = AccessTokenContent(dummyProject.ownerId)
      val request = ProjectAdditionRequest("")

      "return created project" taggedAs Controller in {
        when(projectService.addProject(request, accessToken.userId)).thenReturn(Future.successful(Right(dummyProject)))
        Post("/projects", request) ~> projectController.route(accessToken) ~> check {
          responseAs[Project] shouldBe dummyProject
        }
      }

      "return server error if project addition was failed" taggedAs Controller in {
        when(projectService.addProject(request, accessToken.userId)).thenReturn(
          Future.successful(Left(VersioningException.RepositoryException("VersioningException.RepositoryException")))
        )
        Post("/projects", request) ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
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
      "return status code OK if the update was successful" taggedAs Controller in {
        val userId = TestUserUtils.getDummyUserId
        val accessToken = AccessTokenContent(userId)
        val dummyProject = TestProjectUtils.getDummyProject()
        val request = ProjectUpdateNameRequest(dummyProject.projectId, dummyProject.name)

        when(projectService.updateProjectName(request, userId)).thenReturn(Future.successful(dummyProject.projectId))

        Put("/projects", request) ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
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
