package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dao.utils.{ TestProjectUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.datastorage.dto.{ Project, ProjectAdditionRequest, ProjectUpdateNameRequest }
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
    val dummyProject: Project = TestProjectUtils.getDummyProject()
    val accessToken = AccessTokenContent(dummyProject.ownerId)
    val strangerAccessToken = AccessTokenContent(stranger.userId)

    "get all user projects" should {
      "return list of user projects" taggedAs Controller in {
        when(projectService.getUserProjects(accessToken.userId)).thenReturn(Future.successful(List(dummyProject)))

        Get("/projects") ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[List[Project]] shouldEqual List(dummyProject)
        }
      }

      "return empty list if user projects not found" taggedAs Controller in {
        when(projectService.getUserProjects(accessToken.userId)).thenReturn(Future.successful(Seq()))

        Get("/projects") ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[List[Project]] shouldEqual List()
        }
      }

      "return 500 if service return exception" taggedAs Controller in {
        val error = new RuntimeException("Something went wrong")
        when(projectService.getUserProjects(accessToken.userId)).thenReturn(Future.failed(error))

        Get("/projects") ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }

    "get project by id" should {
      "return project" taggedAs Controller in {
        val accessToken = AccessTokenContent(dummyProject.ownerId)
        when(projectService.getUserProjectById(dummyProject.projectId, accessToken.userId))
          .thenReturn(Future.successful(dummyProject))

        Get(s"/projects/${dummyProject.projectId.value}") ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Project] shouldEqual dummyProject
        }
      }

      "return status code InternalServerError if service returned failure" taggedAs Controller in {
        val accessToken = AccessTokenContent(stranger.userId)
        val error = new RuntimeException("Something went wrong")
        when(projectService.getUserProjectById(dummyProject.projectId, accessToken.userId))
          .thenReturn(Future.failed(error))

        Get(s"/projects/${dummyProject.projectId.value}") ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }

    "get project by name" should {
      "return a object of project type" taggedAs Controller in {
        when(projectService.getUserProjectByName(dummyProject.name, accessToken.userId))
          .thenReturn(Future.successful(dummyProject))

        Get("/projects?name=" + dummyProject.name) ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Project] shouldEqual dummyProject
        }
      }

      "return status code InternalServerError if service returned failure" taggedAs Controller in {
        val accessToken = AccessTokenContent(stranger.userId)
        val error = new RuntimeException("Something went wrong")
        when(projectService.getUserProjectByName(dummyProject.name, accessToken.userId))
          .thenReturn(Future.failed(error))

        Get("/projects?name=" + dummyProject.name) ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }

    "add project" should {
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
        val deactivatedProject = dummyProject.copy(active = false)

        when(
          projectService.deactivateProjectById(dummyProject.projectId, accessToken.userId)
        ).thenReturn(Future.successful(deactivatedProject))

        Delete(s"/projects/${dummyProject.projectId.value}") ~> projectController.route(accessToken) ~> check {
          responseAs[Project] shouldBe deactivatedProject
        }
      }

      "return server error if project deactivation was failed" taggedAs Controller in {
        val projectId = dummyProject.projectId

        when(projectService.deactivateProjectById(projectId, strangerAccessToken.userId))
          .thenReturn(Future.failed(new RuntimeException("Something wrong")))

        Delete(s"/projects/${projectId.value}") ~> projectController.route(strangerAccessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }

    "update project" should {
      "return status code OK if the update was successful" taggedAs Controller in {
        val request = ProjectUpdateNameRequest(dummyProject.name)

        when(projectService.updateProjectName(dummyProject.projectId, request, accessToken.userId))
          .thenReturn(Future.successful(dummyProject.projectId))

        Put(s"/projects/${dummyProject.projectId.value}", request) ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
        }
      }

      "return InternalServerError status if the update failed" taggedAs Controller in {
        val request = ProjectUpdateNameRequest(dummyProject.name)

        when(projectService.updateProjectName(dummyProject.projectId, request, strangerAccessToken.userId))
          .thenReturn(Future.failed(new RuntimeException("Something wrong")))

        Put(s"/projects/${dummyProject.projectId.value}", request) ~> projectController.route(strangerAccessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }
  }
}
