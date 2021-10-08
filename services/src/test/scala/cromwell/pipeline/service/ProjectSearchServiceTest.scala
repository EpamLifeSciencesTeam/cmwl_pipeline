package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.service.ProjectSearchService.Exceptions.InternalError
import cromwell.pipeline.service.impls.{
  ProjectConfigurationServiceTestImpl,
  ProjectFileServiceTestImpl,
  ProjectServiceTestImpl
}
import org.scalatest.{ AsyncWordSpec, Matchers }

import java.nio.file.Paths

class ProjectSearchServiceTest extends AsyncWordSpec with Matchers {

  private val dummyProject1: Project = TestProjectUtils.getDummyProject(name = "First")
  private val projectId: ProjectId = dummyProject1.projectId
  private val userId: UserId = dummyProject1.ownerId
  private val dummyProject2 = TestProjectUtils.getDummyProject(ownerId = userId, name = "Second")
  private val dummyProject3 = TestProjectUtils.getDummyProject(ownerId = userId, name = "Third")
  private val dummyProjects = Seq(dummyProject1, dummyProject2, dummyProject3)

  private val correctWdl = "task hello {}"
  private val projectFilePath = Paths.get("test.txt")
  private val projectFileContent = ProjectFileContent(correctWdl)
  private val projectFile = ProjectFile(projectFilePath, projectFileContent)
  private val dummyProject1FileBundle = (dummyProject1.projectId, Seq(projectFile))
  private val dummyProject2FileBundle = (dummyProject2.projectId, Seq(projectFile))
  private val dummyProject3FileBundle = (dummyProject3.projectId, Seq.empty)

  private val wdlParams: WdlParams =
    WdlParams(Paths.get("/home/file"), List(FileParameter("nodeName", StringTyped(Some("hello")))))
  private val projectConfigurationId = ProjectConfigurationId.randomId

  private val activeConfiguration1: ProjectConfiguration =
    ProjectConfiguration(
      projectConfigurationId,
      projectId,
      active = true,
      wdlParams,
      ProjectConfigurationVersion.defaultVersion
    )
  private val activeConfiguration2: ProjectConfiguration =
    activeConfiguration1.copy(projectId = dummyProject2.projectId)

  private val defaultProjectService = ProjectServiceTestImpl(dummyProjects: _*)
  private val defaultProjectFileService =
    ProjectFileServiceTestImpl(dummyProject1FileBundle, dummyProject2FileBundle, dummyProject3FileBundle)
  private val defaultProjectConfigurationService =
    ProjectConfigurationServiceTestImpl(activeConfiguration1, activeConfiguration2)

  private def createProjectSearchService(
    projectService: ProjectService = defaultProjectService,
    projectFileService: ProjectFileService = defaultProjectFileService,
    projectConfigurationService: ProjectConfigurationService = defaultProjectConfigurationService
  ): ProjectSearchService =
    ProjectSearchService(projectService, projectFileService, projectConfigurationService)

  private val projectSearch = createProjectSearchService()

  "ProjectSearchServiceTest" when {
    "search successfully for all projects" should {
      "return list of all projects" in {
        val request = ProjectSearchRequest(All)
        val result = ProjectSearchResponse(dummyProjects)

        projectSearch.searchProjects(request, userId).map(_ shouldBe result)
      }
    }

    "search project by name full match" should {
      "return full matched project" in {
        val name = dummyProject1.name
        val filter = ByName(FullMatch, name)
        val request = ProjectSearchRequest(filter)
        val result = ProjectSearchResponse(Seq(dummyProject1))

        projectSearch.searchProjects(request, userId).map(_ shouldBe result)
      }
      "return empty result if nothing matched" in {
        val projectName = "UNMATCHED"
        val filter = ByName(RegexpMatch, projectName)
        val request = ProjectSearchRequest(filter)
        val result = ProjectSearchResponse(Seq.empty)

        projectSearch.searchProjects(request, userId).map(_ shouldBe result)
      }
    }

    "search project by name regexp match" should {
      "return matched projects" in {
        val regexString = ".*ir.*"
        val filter = ByName(RegexpMatch, regexString)
        val request = ProjectSearchRequest(filter)
        val result = ProjectSearchResponse(Seq(dummyProject1, dummyProject3))

        projectSearch.searchProjects(request, userId).map(_ shouldBe result)
      }
      "return empty result if nothing matched" in {
        val regexString = ".*UNMATCHED.*"
        val filter = ByName(RegexpMatch, regexString)
        val request = ProjectSearchRequest(filter)
        val result = ProjectSearchResponse(Seq.empty)

        projectSearch.searchProjects(request, userId).map(_ shouldBe result)
      }
    }

    "search project by config" should {
      "return projects with configuration if Exists true" in {
        val filter = ByConfig(mode = Exists, value = true)
        val request = ProjectSearchRequest(filter)
        val result = ProjectSearchResponse(Seq(dummyProject1, dummyProject2))

        projectSearch.searchProjects(request, userId).map(_ shouldBe result)
      }
      "return project without configuration if Exists false" in {
        val filter = ByConfig(mode = Exists, value = false)
        val request = ProjectSearchRequest(filter)
        val result = ProjectSearchResponse(Seq(dummyProject3))

        projectSearch.searchProjects(request, userId).map(_ shouldBe result)
      }
      "fail with exception if unable to fetch configuration" in {
        val failedConfigService = ProjectConfigurationServiceTestImpl.withException(new RuntimeException)
        val projectSearch = createProjectSearchService(projectConfigurationService = failedConfigService)

        val filter = ByConfig(mode = Exists, value = true)
        val request = ProjectSearchRequest(filter)

        projectSearch.searchProjects(request, userId).failed.map {
          _ shouldBe InternalError("Failed to fetch configuration due to unexpected internal error")
        }
      }
    }

    "search project by files" should {
      "return projects with files if Exists true" in {
        val filter = ByFiles(mode = Exists, value = true)
        val request = ProjectSearchRequest(filter)
        val result = ProjectSearchResponse(Seq(dummyProject1, dummyProject2))

        projectSearch.searchProjects(request, userId).map(_ shouldBe result)
      }
      "return project without files if Exists false" in {
        val filter = ByFiles(mode = Exists, value = false)
        val request = ProjectSearchRequest(filter)
        val result = ProjectSearchResponse(Seq(dummyProject3))

        projectSearch.searchProjects(request, userId).map(_ shouldBe result)
      }
      "fail with exception if unable to fetch configuration" in {
        val failedFilesService = ProjectFileServiceTestImpl.withException(new RuntimeException)
        val projectSearch = createProjectSearchService(projectFileService = failedFilesService)

        val filter = ByFiles(mode = Exists, value = true)
        val request = ProjectSearchRequest(filter)

        projectSearch.searchProjects(request, userId).failed.map {
          _ shouldBe InternalError("Failed to fetch files due to unexpected internal error")
        }
      }
    }
    "search project by And filter" should {
      "return only projects suitable for both conditions" in {
        val filterByFiles = ByFiles(mode = Exists, value = true)
        val filterByName = ByName(mode = RegexpMatch, value = ".*ir.*")
        val filterAnd = And(filterByFiles, filterByName)

        val request = ProjectSearchRequest(filterAnd)
        val result = ProjectSearchResponse(Seq(dummyProject1))

        projectSearch.searchProjects(request, userId).map(_ shouldBe result)
      }
    }

    "search project with complex filter" should {
      "return filtered projects" in {
        val filterByNameFullMatch = ByName(FullMatch, dummyProject3.name)
        val filterConfigExist = ByConfig(mode = Exists, value = true)
        val filterFilesExist = ByFiles(mode = Exists, value = true)
        val filterFilesNotExist = ByFiles(mode = Exists, value = false)
        val filterBothFilesAndConfigsExist = And(filterFilesExist, filterConfigExist)
        val filterFullNameMatchAndFilesNotExist = And(filterByNameFullMatch, filterFilesNotExist)
        val complexFilter = Or(filterBothFilesAndConfigsExist, filterFullNameMatchAndFilesNotExist)

        val request = ProjectSearchRequest(complexFilter)
        val result = ProjectSearchResponse(Seq(dummyProject1, dummyProject2, dummyProject3))

        projectSearch.searchProjects(request, userId).map(_ shouldBe result)
      }
    }

    "do any search" should {
      "fail with exception if unable to fetch user projects" in {
        val failedProjectService = ProjectServiceTestImpl.withException(new RuntimeException)
        val projectSearch = createProjectSearchService(projectService = failedProjectService)
        val request = ProjectSearchRequest(All)

        projectSearch.searchProjects(request, userId).failed.map {
          _ shouldBe InternalError("Failed to fetch projects due to unexpected internal error")
        }
      }
    }
  }
}
