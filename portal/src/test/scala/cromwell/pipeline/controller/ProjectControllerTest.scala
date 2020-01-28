package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dto.Project
import cromwell.pipeline.service.ProjectService
import cromwell.pipeline.tag.Controller
import cromwell.pipeline.utils.auth.{ AccessTokenContent, TestProjectUtils }
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
        val projectId = dummyProject.projectId
        val getProjectByNameResponse: Option[Project] = Option(dummyProject)

        val accessToken = AccessTokenContent(projectId.value)
        when(projectService.getProjectByName(projectByName)).thenReturn(Future.successful(getProjectByNameResponse))

        Get("/projects?name=" + projectByName) ~> projectController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Option[Project]] shouldEqual (getProjectByNameResponse)
        }
      }
    }
  }
}