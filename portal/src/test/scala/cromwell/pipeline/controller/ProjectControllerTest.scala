package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dto.{ Project, ProjectId, User, UserId }
import cromwell.pipeline.service.{ ProjectAccessDeniedException, ProjectNotFoundException, ProjectService }
import cromwell.pipeline.tag.Controller
import cromwell.pipeline.utils.auth.{ AccessTokenContent, TestProjectUtils, TestUserUtils }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
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

  "DELETE project by id" should {
    "return deactivated project entity" in {
      val dummyUser: User = TestUserUtils.getDummyUser()
      val dummyProject: Project = TestProjectUtils.getDummyProject()
      val accessToken = AccessTokenContent(dummyUser.userId.value)
      val deactivatedProject = dummyProject.copy(active = false)

      when(projectService.deactivateProjectById(dummyProject.projectId, UserId(accessToken.userId)))
        .thenReturn(Future.successful(Some(deactivatedProject)))

      Delete("/projects") ~> projectController.route(accessToken) ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Project] shouldBe deactivatedProject
      }
    }
  }

}
