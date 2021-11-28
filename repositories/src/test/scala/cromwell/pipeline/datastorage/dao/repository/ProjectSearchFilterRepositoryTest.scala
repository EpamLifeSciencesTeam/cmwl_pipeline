package cromwell.pipeline.datastorage.dao.repository

import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.datastorage.DatastorageModule
import cromwell.pipeline.datastorage.dao.utils.PostgreTablesCleaner
import cromwell.pipeline.datastorage.dto.{ ByName, FullMatch, ProjectSearchFilter }
import cromwell.pipeline.model.wrapper.ProjectSearchFilterId
import cromwell.pipeline.utils.{ ApplicationConfig, TestContainersUtils }
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, Matchers }

import java.time.Instant
import java.time.temporal.ChronoUnit

class ProjectSearchFilterRepositoryTest
    extends AsyncWordSpec
    with Matchers
    with BeforeAndAfterAll
    with ForAllTestContainer
    with PostgreTablesCleaner {

  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  protected lazy val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  protected lazy val datastorageModule: DatastorageModule = new DatastorageModule(ApplicationConfig.load(config))

  override protected def beforeAll(): Unit = {
    super.beforeAll
    datastorageModule.pipelineDatabaseEngine.updateSchema()
  }

  import datastorageModule.projectSearchFilterRepository

  "ProjectSearchFilterRepository" when {

    "getProjectSearchFilterById" should {

      "find added projectSearchFilter by id" taggedAs Dao in {
        val query = ByName(mode = FullMatch, value = "Hello")
        val projectSearchFilter = ProjectSearchFilter(ProjectSearchFilterId.random, query, Instant.now)

        val result = for {
          filterId <- projectSearchFilterRepository.addProjectSearchFilter(projectSearchFilter)
          filter <- projectSearchFilterRepository.getProjectSearchFilterById(filterId)
        } yield filter

        result.map(_ shouldBe Some(projectSearchFilter))
      }
    }

    "getProjectSearchFilterById" should {

      "not find expired projectSearchFilter" in {
        val query = ByName(mode = FullMatch, value = "Hello")
        val currentTime = Instant.now
        val allowedTtlInSec = 15
        val expiredTtlInSec = 30

        val lastAllowedUseTime = currentTime.minus(allowedTtlInSec, ChronoUnit.MINUTES)
        val expiredLastUsedTime = currentTime.minus(expiredTtlInSec, ChronoUnit.MINUTES)
        val expiredProjectSearchFilter = ProjectSearchFilter(ProjectSearchFilterId.random, query, expiredLastUsedTime)

        val result = for {
          expiredFilterId <- projectSearchFilterRepository.addProjectSearchFilter(expiredProjectSearchFilter)
          _ <- projectSearchFilterRepository.deleteOldFilters(lastAllowedUseTime)
          search <- projectSearchFilterRepository.getProjectSearchFilterById(expiredFilterId)
        } yield search

        result.map(_ shouldBe None)
      }
    }

    "update lastUsedAt" should {

      "update lastUsedAt of project search filter" in {
        val query = ByName(mode = FullMatch, value = "Hello")
        val expiredTtlInSec = 30
        val passedTime = Instant.now.minus(expiredTtlInSec, ChronoUnit.SECONDS)
        val currentTime = Instant.now
        val searchId = ProjectSearchFilterId.random
        val projectSearch = ProjectSearchFilter(searchId, query, passedTime)
        val expectedResult = Some(ProjectSearchFilter(searchId, query, currentTime))

        val result = for {
          searchId <- projectSearchFilterRepository.addProjectSearchFilter(projectSearch)
          _ <- projectSearchFilterRepository.updateLastUsedAt(searchId, currentTime)
          search <- projectSearchFilterRepository.getProjectSearchFilterById(searchId)
        } yield search

        result.map(_ shouldBe expectedResult)
      }
    }
  }

}
