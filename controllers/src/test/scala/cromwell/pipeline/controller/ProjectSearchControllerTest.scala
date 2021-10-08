package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dao.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.service.impls.ProjectSearchServiceTestImpl
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import org.scalatest.{ AsyncWordSpec, Matchers }

class ProjectSearchControllerTest extends AsyncWordSpec with Matchers with ScalatestRouteTest {

  private val dummyProject = TestProjectUtils.getDummyProject()
  private val projects = Seq(dummyProject)
  private val accessToken = AccessTokenContent(dummyProject.ownerId)
  private val filter = ByConfig(Exists, value = true)
  private val request = ProjectSearchRequest(filter)

  "ProjectSearchController" when {
    "search projects" should {
      "Return successful response with seq of projects" in {
        val searchResponse: ProjectSearchResponse = ProjectSearchResponse(projects)
        val projectSearchService = ProjectSearchServiceTestImpl(dummyProject)
        val projectSearchController = new ProjectSearchController(projectSearchService)

        Post("/projects/search", request) ~> projectSearchController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[ProjectSearchResponse] shouldEqual searchResponse
        }
      }

      "Return successful response with empty seq of projects if no projects was found" in {
        val searchResponse: ProjectSearchResponse = ProjectSearchResponse(Seq.empty)
        val emptyProjectSearchService = ProjectSearchServiceTestImpl()
        val projectSearchController = new ProjectSearchController(emptyProjectSearchService)

        Post("/projects/search", request) ~> projectSearchController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[ProjectSearchResponse] shouldEqual searchResponse
        }
      }

      "Return InternalServerError if service fails" in {
        val failProjectSearchService = ProjectSearchServiceTestImpl.withException(new RuntimeException)
        val projectSearchController = new ProjectSearchController(failProjectSearchService)

        Post("/projects/search", request) ~> projectSearchController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }
  }
}
