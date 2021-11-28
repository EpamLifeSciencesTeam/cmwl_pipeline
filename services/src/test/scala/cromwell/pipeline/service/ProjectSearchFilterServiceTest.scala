package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.impls.ProjectSearchFilterRepositoryTestImpl
import cromwell.pipeline.datastorage.dto.{ All, ProjectSearchFilter }
import cromwell.pipeline.model.wrapper.ProjectSearchFilterId
import cromwell.pipeline.service.ProjectSearchFilterService.Exceptions._
import org.scalatest.{ AsyncWordSpec, Matchers }

import java.time.Instant
class ProjectSearchFilterServiceTest extends AsyncWordSpec with Matchers {

  private val query = All
  private val currentTime = Instant.now
  private val projectSearchFilter = ProjectSearchFilter(ProjectSearchFilterId.random, query, currentTime)

  "ProjectSearchFilterServiceTest" when {
    "add search filter" should {
      "run successfully" in {
        val projectSearchRepository = ProjectSearchFilterRepositoryTestImpl(projectSearchFilter)
        val projectSearchFilterService = ProjectSearchFilterService(projectSearchRepository)

        projectSearchFilterService.addProjectSearchFilter(query).map(_ => succeed)
      }
      "fail with internal error if projectSearchFilterRepository fails" in {

        val failedPprojectSearchRepository = ProjectSearchFilterRepositoryTestImpl.withException(new RuntimeException)
        val projectSearchFilterService = ProjectSearchFilterService(failedPprojectSearchRepository)

        projectSearchFilterService.addProjectSearchFilter(query).failed.map {
          _ shouldBe InternalError("Failed to add filter due to unexpected internal error")
        }
      }
    }

    "get search filter by id" should {
      "return query" in {
        val projectSearchRepository = ProjectSearchFilterRepositoryTestImpl(projectSearchFilter)
        val projectSearchFilterService = ProjectSearchFilterService(projectSearchRepository)

        projectSearchFilterService.getSearchFilterById(projectSearchFilter.id).map(_ shouldBe projectSearchFilter)
      }
      "fail with internal error if projectSearchRepository fails" in {

        val failedProjectSearchRepository = ProjectSearchFilterRepositoryTestImpl.withException(new RuntimeException)
        val projectSearchFilterService = ProjectSearchFilterService(failedProjectSearchRepository)

        projectSearchFilterService.getSearchFilterById(projectSearchFilter.id).failed.map {
          _ shouldBe InternalError("Failed to find filter due to unexpected internal error")
        }
      }
      "fail with NotFound if projectSearchFilter is not found" in {

        val failedProjectSearchRepository = ProjectSearchFilterRepositoryTestImpl()
        val projectSearchFilterService = ProjectSearchFilterService(failedProjectSearchRepository)

        projectSearchFilterService.getSearchFilterById(projectSearchFilter.id).failed.map {
          _ shouldBe NotFound()
        }
      }
    }

    "update lastUsedAt" should {
      "return unit if succeeded" in {
        val projectSearchRepository = ProjectSearchFilterRepositoryTestImpl(projectSearchFilter)
        val projectSearchFilterService = ProjectSearchFilterService(projectSearchRepository)

        projectSearchFilterService.updateLastUsedAt(projectSearchFilter.id, currentTime).map(_ shouldBe ((): Unit))
      }
      "fail with internal error if projectSearchFilterService fails" in {
        val failedProjectSearchRepository = ProjectSearchFilterRepositoryTestImpl.withException(new RuntimeException)
        val projectSearchFilterService = ProjectSearchFilterService(failedProjectSearchRepository)

        projectSearchFilterService.updateLastUsedAt(projectSearchFilter.id, currentTime).failed.map {
          _ shouldBe InternalError("Failed to update lastUsedAt due to unexpected internal error")
        }
      }
    }
  }
}
