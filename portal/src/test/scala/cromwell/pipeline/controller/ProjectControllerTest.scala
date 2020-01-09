package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dto.{ Project, ProjectCreationRequest, User, UserId }
import cromwell.pipeline.service.{ ProjectDeactivationForbiddenException, ProjectNotFoundException, ProjectService }
import cromwell.pipeline.utils.auth.{ AccessTokenContent, TestUserUtils }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import org.mockito.Mockito._
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class ProjectControllerTest extends AsyncWordSpec with Matchers with MockitoSugar with ScalatestRouteTest {

  private val projectService = mock[ProjectService]
  private val projectController = new ProjectController(projectService)

  "ProjectController" when {
    "GET project by id" should {
      "return project entity" in {
        val dummyUser: User = TestUserUtils.getDummyUser(active = false)
        val dummyProject = TestUserUtils.getDummyProject(dummyUser.userId)
        val accessToken = AccessTokenContent(dummyUser.userId.value)

        when(projectService.getProjectById(dummyProject.projectId)).thenReturn(Future.successful(Some(dummyProject)))

        Get(s"/projects/${dummyProject.projectId.value}") ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Project] shouldBe dummyProject
        }
      }

      "return '404 Not Found' if there is no project with such id" in {
        val dummyUser: User = TestUserUtils.getDummyUser(active = false)
        val dummyProject = TestUserUtils.getDummyProject(dummyUser.userId)
        val accessToken = AccessTokenContent(dummyUser.userId.value)

        when(projectService.getProjectById(dummyProject.projectId)).thenReturn(Future.successful(None))

        Get(s"/projects/${dummyProject.projectId.value}") ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NotFound
        }
      }
    }

    "DELETE project by id" should {
      "return deactivated project entity" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val dummyProject = TestUserUtils.getDummyProject(dummyUser.userId)
        val accessToken = AccessTokenContent(dummyUser.userId.value)
        val deactivatedProject = dummyProject.copy(active = false)

        when(projectService.deactivateProjectById(dummyProject.projectId, UserId(accessToken.userId)))
          .thenReturn(Future.successful(Some(deactivatedProject)))

        Delete(s"/projects/${dummyProject.projectId.value}") ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Project] shouldBe deactivatedProject
        }
      }

      "return '404 Not Found' if there is no project with such id" in {
        val dummyUser: User = TestUserUtils.getDummyUser(active = false)
        val dummyProject = TestUserUtils.getDummyProject(dummyUser.userId)
        val accessToken = AccessTokenContent(dummyUser.userId.value)

        when(projectService.deactivateProjectById(dummyProject.projectId, UserId(accessToken.userId)))
          .thenReturn(Future.failed(new ProjectNotFoundException))

        Delete(s"/projects/${dummyProject.projectId.value}") ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NotFound
        }
      }

      "return '403 Forbidden' if user is not an owner of that project" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val dummyProject = TestUserUtils.getDummyProject(dummyUser.userId)
        val accessToken = AccessTokenContent("non-owner-userid")

        when(projectService.deactivateProjectById(dummyProject.projectId, UserId(accessToken.userId)))
          .thenReturn(Future.failed(new ProjectDeactivationForbiddenException))

        Delete(s"/projects/${dummyProject.projectId.value}") ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.Forbidden
          responseAs[String] shouldBe ProjectController.PROJECT_DEACTIVATION_FORBIDDEN_MESSAGE
        }
      }
    }

    "POST new project" should {
      "return id of added project" in {
        val dummyUser: User = TestUserUtils.getDummyUser(active = false)
        val userId = dummyUser.userId
        val dummyProject = TestUserUtils.getDummyProject(userId)
        val projectCreationRequest = ProjectCreationRequest(
          ownerId = dummyProject.ownerId,
          name = dummyProject.name,
          repository = dummyProject.repository
        )
        val accessToken = AccessTokenContent(userId.value)

        when(projectService.addProject(projectCreationRequest)).thenReturn(Future.successful(dummyProject.projectId))

        Post(s"/projects/", projectCreationRequest) ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[String] shouldBe dummyProject.projectId.value
        }
      }
    }
  }
}
