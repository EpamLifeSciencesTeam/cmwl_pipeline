package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dao.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.model.wrapper.ProjectSearchFilterId
import cromwell.pipeline.service.ProjectSearchService.Exceptions._
import cromwell.pipeline.service.impls.ProjectSearchServiceTestImpl
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import org.scalatest.{ AsyncWordSpec, Matchers }

class ProjectSearchControllerTest extends AsyncWordSpec with Matchers with ScalatestRouteTest {

  private val dummyProject = TestProjectUtils.getDummyProject()
  private val projects = Seq(dummyProject)
  private val accessToken = AccessTokenContent(dummyProject.ownerId)
  private val query = ByConfig(Exists, value = true)
  private val request = ProjectSearchRequest(query)
  private val filterId = ProjectSearchFilterId.random

  "ProjectSearchController" when {
    "search projects by new query" should {
      "Return successful response with seq of projects" in {
        val projectSearchService = ProjectSearchServiceTestImpl(dummyProject)
        val projectSearchController = new ProjectSearchController(projectSearchService)

        Post("/projects/search", request) ~> projectSearchController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[ProjectSearchResponse].data shouldEqual projects
        }
      }

      "Return successful response with empty seq of projects if no projects were found" in {
        val emptyProjectSearchService = ProjectSearchServiceTestImpl()
        val projectSearchController = new ProjectSearchController(emptyProjectSearchService)

        Post("/projects/search", request) ~> projectSearchController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[ProjectSearchResponse].data shouldEqual Seq.empty
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

    "search projects by filter Id" should {
      "Return successful response with seq of projects" in {
        val projectSearchService = ProjectSearchServiceTestImpl(dummyProject)
        val projectSearchController = new ProjectSearchController(projectSearchService)

        Get(s"/projects/search/$filterId") ~> projectSearchController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[ProjectSearchResponse].data shouldEqual projects
        }
      }

      "Return successful response with empty seq of projects if no projects were found" in {
        val emptyProjectSearchService = ProjectSearchServiceTestImpl()
        val projectSearchController = new ProjectSearchController(emptyProjectSearchService)

        Get(s"/projects/search/$filterId") ~> projectSearchController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[ProjectSearchResponse].data shouldEqual Seq.empty
        }
      }

      "Return 404 if no filter was found by id" in {
        val emptyProjectSearchService = ProjectSearchServiceTestImpl.withException(NotFound())
        val projectSearchController = new ProjectSearchController(emptyProjectSearchService)

        Get(s"/projects/search/$filterId") ~> projectSearchController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NotFound
        }
      }

      "Return InternalServerError if search fails" in {
        val failProjectSearchService = ProjectSearchServiceTestImpl.withException(new RuntimeException)
        val projectSearchController = new ProjectSearchController(failProjectSearchService)

        Get(s"/projects/search/$filterId") ~> projectSearchController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }
  }
}
