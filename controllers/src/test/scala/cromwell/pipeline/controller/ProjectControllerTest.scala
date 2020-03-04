package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dao.repository.utils.{TestProjectUtils, TestUserUtils}
import cromwell.pipeline.datastorage.dto.{Project, ProjectDeleteRequest, ProjectUpdateRequest, UserId}
import cromwell.pipeline.datastorage.utils.auth.AccessTokenContent
import cromwell.pipeline.service.ProjectService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.mockito.Mockito.when
import org.scalatest.{AsyncWordSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class ProjectControllerTest
  extends AsyncWordSpec
    with Matchers
    with ScalatestRouteTest
    with MockitoSugar
    with PlayJsonSupport {

  private val projectService = mock[ProjectService]
  private val projectController = new ProjectController(projectService)

  "Project controller" when {
    "get project by name" should {
      "return a object of project type" taggedAs (Controller) in {
        val projectByName: String = "dummyProject"
        val dummyProject: Project = TestProjectUtils.getDummyProject()
        val getProjectByNameResponse: Option[Project] = Option(dummyProject)
        val accessToken = AccessTokenContent(dummyProject.ownerId.value)

        when(projectService.getProjectByName(projectByName, new UserId(accessToken.userId)))
          .thenReturn(Future.successful(getProjectByNameResponse))

        Get("/projects?name=" + projectByName) ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Option[Project]] shouldEqual (getProjectByNameResponse)
        }
      }
    }
  }

  "delete project by id" should {
    "return deactivated project entity" in {
      val dummyProject = TestProjectUtils.getDummyProject()
      val accessToken = AccessTokenContent(TestUserUtils.getDummyUser().userId.value)
      val deactivatedProject = dummyProject.copy(active = false)
      val request = ProjectDeleteRequest(dummyProject.projectId)

      when(projectService.deactivateProjectById(dummyProject.projectId, UserId(accessToken.userId)))
        .thenReturn(Future.successful(Some(deactivatedProject)))

      Delete("/projects", request) ~> projectController.route(accessToken) ~> check {
        responseAs[Project] shouldBe deactivatedProject
      }

    }

    "return server error if project deactivation was failed" taggedAs (Controller) in {
      val userId = TestUserUtils.getDummyUser().userId
      val request = ProjectDeleteRequest(TestProjectUtils.getDummyProject().projectId)
      val accessToken = AccessTokenContent(userId.value)

      when(projectService.deactivateProjectById(request.projectId, userId))
        .thenReturn(Future.failed(new RuntimeException("Something wrong")))

      Delete("/projects", request) ~> projectController.route(accessToken) ~> check {
        status shouldBe StatusCodes.InternalServerError
      }
    }

  }

  "update project" should {
    "return status code NoContent if " in {
      val userId = TestUserUtils.getDummyUser().userId
      val accessToken = AccessTokenContent(userId.value)
      val dummyProject = TestProjectUtils.getDummyProject()
      val request = ProjectUpdateRequest(dummyProject.projectId, dummyProject.name, dummyProject.repository)

      when(projectService.updateProject(request, userId)).thenReturn(Future.successful(1))

      Put("/projects", request) ~> projectController.route(accessToken) ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }

    "return InternalServerError status if project id doesn't match" in {
      val userId = TestUserUtils.getDummyUser().userId
      val accessToken = AccessTokenContent("0")
      val dummyProject = TestProjectUtils.getDummyProject()
      val request = ProjectUpdateRequest(dummyProject.projectId, dummyProject.name, dummyProject.repository)

      when(projectService.updateProject(request, userId))
        .thenReturn(Future.failed(new RuntimeException("Something wrong")))

      Put("/projects", request) ~> projectController.route(accessToken) ~> check {
        status shouldBe StatusCodes.InternalServerError
      }
    }

  }

}
