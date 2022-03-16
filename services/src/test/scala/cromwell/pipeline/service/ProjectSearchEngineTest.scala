package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.validator.Enable
import cromwell.pipeline.model.wrapper.{ ProjectConfigurationId, ProjectId, UserId }
import cromwell.pipeline.service.ProjectSearchEngine.Exceptions.InternalError
import cromwell.pipeline.service.impls.{
  ProjectConfigurationServiceTestImpl,
  ProjectFileServiceTestImpl,
  ProjectServiceTestImpl
}
import org.scalatest.{ AsyncWordSpec, Matchers }

import java.nio.file.Paths
import java.util.UUID

class ProjectSearchEngineTest extends AsyncWordSpec with Matchers {

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
  private val projectConfigurationId = ProjectConfigurationId(UUID.randomUUID().toString, Enable.Unsafe)

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

  private def createProjectSearchEngine(
    projectService: ProjectService = defaultProjectService,
    projectFileService: ProjectFileService = defaultProjectFileService,
    projectConfigurationService: ProjectConfigurationService = defaultProjectConfigurationService
  ): ProjectSearchEngine =
    ProjectSearchEngine(projectService, projectFileService, projectConfigurationService)

  private val projectSearchEngine = createProjectSearchEngine()

  "ProjectSearchServiceTest" when {
    "search successfully for all projects" should {
      "return list of all projects" in {
        val query = All
        val result = dummyProjects

        projectSearchEngine.searchProjects(query, userId).map(_ shouldBe result)
      }
    }

    "search project by name full match" should {
      "return full matched project" in {
        val name = dummyProject1.name
        val query = ByName(FullMatch, name)
        val result = Seq(dummyProject1)

        projectSearchEngine.searchProjects(query, userId).map(_ shouldBe result)
      }
      "return empty result if nothing matched" in {
        val projectName = "UNMATCHED"
        val query = ByName(RegexpMatch, projectName)
        val result = Seq.empty

        projectSearchEngine.searchProjects(query, userId).map(_ shouldBe result)
      }
    }

    "search project by name regexp match" should {
      "return matched projects" in {
        val regexString = ".*ir.*"
        val query = ByName(RegexpMatch, regexString)
        val result = Seq(dummyProject1, dummyProject3)

        projectSearchEngine.searchProjects(query, userId).map(_ shouldBe result)
      }
      "return empty result if nothing matched" in {
        val regexString = ".*UNMATCHED.*"
        val query = ByName(RegexpMatch, regexString)
        val result = Seq.empty

        projectSearchEngine.searchProjects(query, userId).map(_ shouldBe result)
      }
    }

    "search project by config" should {
      "return projects with configuration if Exists true" in {
        val query = ByConfig(mode = Exists, value = true)
        val result = Seq(dummyProject1, dummyProject2)

        projectSearchEngine.searchProjects(query, userId).map(_ shouldBe result)
      }
      "return project without configuration if Exists false" in {
        val query = ByConfig(mode = Exists, value = false)
        val result = Seq(dummyProject3)

        projectSearchEngine.searchProjects(query, userId).map(_ shouldBe result)
      }
      "fail with exception if unable to fetch configuration" in {
        val failedConfigService = ProjectConfigurationServiceTestImpl.withException(new RuntimeException)
        val projectSearch = createProjectSearchEngine(projectConfigurationService = failedConfigService)

        val query = ByConfig(mode = Exists, value = true)

        projectSearch.searchProjects(query, userId).failed.map {
          _ shouldBe InternalError("Failed to fetch configuration due to unexpected internal error")
        }
      }
    }

    "search project by files" should {
      "return projects with files if Exists true" in {
        val query = ByFiles(mode = Exists, value = true)

        val result = Seq(dummyProject1, dummyProject2)

        projectSearchEngine.searchProjects(query, userId).map(_ shouldBe result)
      }
      "return project without files if Exists false" in {
        val query = ByFiles(mode = Exists, value = false)
        val result = Seq(dummyProject3)

        projectSearchEngine.searchProjects(query, userId).map(_ shouldBe result)
      }
      "fail with exception if unable to fetch configuration" in {
        val failedFilesService = ProjectFileServiceTestImpl.withException(new RuntimeException)
        val projectSearch = createProjectSearchEngine(projectFileService = failedFilesService)

        val query = ByFiles(mode = Exists, value = true)

        projectSearch.searchProjects(query, userId).failed.map {
          _ shouldBe InternalError("Failed to fetch files due to unexpected internal error")
        }
      }
    }
    "search project by And query" should {
      "return only projects suitable for both conditions" in {
        val byFiles = ByFiles(mode = Exists, value = true)
        val byName = ByName(mode = RegexpMatch, value = ".*ir.*")
        val andQuery = And(byFiles, byName)

        val result = Seq(dummyProject1)

        projectSearchEngine.searchProjects(andQuery, userId).map(_ shouldBe result)
      }
    }

    "search project with complex query" should {
      "return filtered projects" in {
        val byNameFullMatch = ByName(FullMatch, dummyProject3.name)
        val byConfigExist = ByConfig(mode = Exists, value = true)
        val byFilesExist = ByFiles(mode = Exists, value = true)
        val byFilesNotExist = ByFiles(mode = Exists, value = false)
        val byBothFilesAndConfigsExist = And(byFilesExist, byConfigExist)
        val byFullNameMatchAndFilesNotExist = And(byNameFullMatch, byFilesNotExist)
        val complexOrQuery = Or(byBothFilesAndConfigsExist, byFullNameMatchAndFilesNotExist)

        val result = Seq(dummyProject1, dummyProject2, dummyProject3)

        projectSearchEngine.searchProjects(complexOrQuery, userId).map(_ shouldBe result)
      }
    }

    "do any search" should {
      "fail with exception if unable to fetch user projects" in {
        val failedProjectService = ProjectServiceTestImpl.withException(new RuntimeException)
        val projectSearch = createProjectSearchEngine(projectService = failedProjectService)
        val query = All

        projectSearch.searchProjects(query, userId).failed.map {
          _ shouldBe InternalError("Failed to fetch projects due to unexpected internal error")
        }
      }
    }
  }
}
