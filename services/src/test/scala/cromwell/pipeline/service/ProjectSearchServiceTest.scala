package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.{ ProjectSearchFilterId, UserId }
import cromwell.pipeline.service.ProjectSearchService.Exceptions._
import cromwell.pipeline.service.impls.{
  ProjectSearchEngineTestImpl,
  ProjectSearchFilterServiceTestImpl,
  ProjectServiceTestImpl
}
import org.scalatest.{ AsyncWordSpec, Matchers }

import java.time.Instant

class ProjectSearchServiceTest extends AsyncWordSpec with Matchers {

  private val dummyProject1: Project = TestProjectUtils.getDummyProject(name = "First")
  private val userId: UserId = dummyProject1.ownerId
  private val dummyProject2 = TestProjectUtils.getDummyProject(ownerId = userId, name = "Second")
  private val dummyProject3 = TestProjectUtils.getDummyProject(ownerId = userId, name = "Third")
  private val dummyProjects = Seq(dummyProject1, dummyProject2, dummyProject3)
  private val filterId = ProjectSearchFilterId.random
  private val currentTime = Instant.now
  private val filter = ProjectSearchFilter(filterId, All, currentTime)
  private val request = ProjectSearchRequest(All)

  private val defaultProjectService = ProjectServiceTestImpl(dummyProjects: _*)
  private val defaultFilterService = ProjectSearchFilterServiceTestImpl(filter)
  private val defaultProjectSearchEngine = ProjectSearchEngineTestImpl(dummyProjects: _*)

  private def createProjectSearchService(
    projectService: ProjectService = defaultProjectService,
    filterService: ProjectSearchFilterService = defaultFilterService,
    searchEngine: ProjectSearchEngine = defaultProjectSearchEngine
  ): ProjectSearchService =
    ProjectSearchService(projectService, filterService, searchEngine)

  private val projectSearchService = createProjectSearchService()

  "ProjectSearchServiceTest" when {
    "searchProjectsByNewFilter" should {
      "return response with new id and seq of projects" in {
        val resultProjects = dummyProjects

        projectSearchService.searchProjectsByNewQuery(request, userId).map(_.data shouldBe resultProjects)
      }
      "fail with exception if unable to add new filter" in {
        val projectSearchService = createProjectSearchService(
          filterService = ProjectSearchFilterServiceTestImpl.withException(new RuntimeException)
        )

        projectSearchService.searchProjectsByNewQuery(request, userId).failed.map {
          _ shouldBe InternalError("Failed to save filter due to unexpected internal error")
        }
      }
      "fail with exception if searchEngine fails" in {
        val projectSearchService = createProjectSearchService(
          searchEngine = ProjectSearchEngineTestImpl.withException(new RuntimeException)
        )

        projectSearchService.searchProjectsByNewQuery(request, userId).failed.map {
          _ shouldBe InternalError("Failed to process search query due to unexpected internal error")
        }
      }
    }

    "searchProjectsByFilterId" should {
      "return response with id and seq of projects" in {
        val result = ProjectSearchResponse(filterId, dummyProjects)

        projectSearchService.searchProjectsByFilterId(filterId, userId).map(_ shouldBe result)
      }
      "fail with exception if filter is not found" in {
        val projectSearchService = createProjectSearchService(
          filterService = ProjectSearchFilterServiceTestImpl()
        )

        projectSearchService.searchProjectsByFilterId(filterId, userId).failed.map {
          _ shouldBe NotFound()
        }
      }
      "fail with exception if searchEngine fails" in {
        val projectSearchService = createProjectSearchService(
          searchEngine = ProjectSearchEngineTestImpl.withException(new RuntimeException)
        )

        projectSearchService.searchProjectsByNewQuery(request, userId).failed.map {
          _ shouldBe InternalError("Failed to process search query due to unexpected internal error")
        }
      }
      "fail with exception if filter timestart update fails" in {
        val projectSearchService = createProjectSearchService(
          searchEngine = ProjectSearchEngineTestImpl.withException(new RuntimeException)
        )

        projectSearchService.searchProjectsByNewQuery(request, userId).failed.map {
          _ shouldBe InternalError("Failed to process search query due to unexpected internal error")
        }
      }
    }
  }
}
