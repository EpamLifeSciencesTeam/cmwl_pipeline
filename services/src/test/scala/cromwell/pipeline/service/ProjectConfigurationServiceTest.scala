package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.ProjectConfigurationRepository
import cromwell.pipeline.datastorage.dao.repository.impls.ProjectConfigurationRepositoryTestImpl
import cromwell.pipeline.datastorage.dao.utils.{ TestProjectUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.service.ProjectService.Exceptions.AccessDenied
import cromwell.pipeline.service.impls.{ ProjectServiceTestImpl, ProjectVersioningTestImpl, WomToolTestImpl }
import cromwell.pipeline.womtool.WomToolAPI
import org.scalatest.{ AsyncWordSpec, Matchers }

import java.nio.file.{ Path, Paths }

class ProjectConfigurationServiceTest extends AsyncWordSpec with Matchers {

  private val dummyProject: Project = TestProjectUtils.getDummyProject()
  private val projectId: ProjectId = dummyProject.projectId
  private val userId: UserId = dummyProject.ownerId
  private val strangerId: UserId = TestUserUtils.getDummyUserId
  private val projectConfigurationId = ProjectConfigurationId.randomId
  private val correctWdl = "task hello {}"
  private val projectFilePath: Path = Paths.get("test.txt")
  private val projectFileContent: ProjectFileContent = ProjectFileContent(correctWdl)
  private val projectFile: ProjectFile = ProjectFile(projectFilePath, projectFileContent)
  private val version: PipelineVersion = TestProjectUtils.getDummyPipeLineVersion()
  private val optionVersion: Option[PipelineVersion] = Some(version)
  private val errorMessage = "ERROR: miss bracket"

  private val wdlParams: WdlParams =
    WdlParams(Paths.get("/home/file"), List(FileParameter("nodeName", StringTyped(Some("hello")))))

  private val activeConfiguration: ProjectConfiguration =
    ProjectConfiguration(
      projectConfigurationId,
      projectId,
      active = true,
      wdlParams,
      ProjectConfigurationVersion.defaultVersion
    )

  private val inactiveConfiguration: ProjectConfiguration =
    activeConfiguration.copy(active = false)

  "ProjectConfigurationService" when {
    val defaultProjectVersioning: ProjectVersioningTestImpl =
      ProjectVersioningTestImpl(projectFiles = List(projectFile), pipelineVersions = List(version))
    val defaultProjectService: ProjectServiceTestImpl = ProjectServiceTestImpl(dummyProject)
    val defaultWomTool: WomToolAPI = WomToolTestImpl()
    def defaultConfigurationRepository: ProjectConfigurationRepositoryTestImpl =
      ProjectConfigurationRepositoryTestImpl(activeConfiguration)

    def createConfigurationService(
      projectConfigurationRepository: ProjectConfigurationRepository = defaultConfigurationRepository,
      projectService: ProjectService = defaultProjectService,
      womTool: WomToolAPI = defaultWomTool,
      projectVersioning: ProjectVersioning[VersioningException] = defaultProjectVersioning
    ): ProjectConfigurationService =
      ProjectConfigurationService(projectConfigurationRepository, projectService, womTool, projectVersioning)

    def configurationService = createConfigurationService()

    "add new configuration for project and user is project owner" should {
      val result: Unit = ()
      val error = new Exception("Oh no")

      "return success if creation was successful" in {
        configurationService.addConfiguration(activeConfiguration, userId).map(_ shouldBe result)
      }

      "return failure if creation wasn't successful" in {
        val configurationService = createConfigurationService(
          projectConfigurationRepository = ProjectConfigurationRepositoryTestImpl.withException(error)
        )
        configurationService.addConfiguration(activeConfiguration, userId).failed.map(_ shouldBe error)
      }
    }

    "add new configuration for project and user is not project owner" should {
      "return failure" in {
        val error = AccessDenied()

        val configurationService = createConfigurationService(
          projectService = ProjectServiceTestImpl.withException(error)
        )
        configurationService.addConfiguration(activeConfiguration, strangerId).failed.map(_ shouldBe error)
      }
    }

    "get configuration by project id and user is project owner" should {
      "return project if it was found" in {
        val result: Option[ProjectConfiguration] = Some(activeConfiguration)

        configurationService.getLastByProjectId(projectId, userId).map(_ shouldBe result)
      }

      "not return inactive project" in {
        val result: Seq[ProjectConfiguration] = Seq(inactiveConfiguration)

        val configurationService = createConfigurationService(
          projectConfigurationRepository = ProjectConfigurationRepositoryTestImpl(result: _*)
        )
        configurationService.getLastByProjectId(projectId, userId).map(_ shouldBe None)
      }

      "not fail if project wasn't found" in {
        val result: Option[ProjectConfiguration] = None
        val configurationService = createConfigurationService(
          projectConfigurationRepository = ProjectConfigurationRepositoryTestImpl()
        )
        configurationService.getLastByProjectId(projectId, userId).map(_ shouldBe result)
      }

      "return failure if repository returned error" in {
        val error = new Exception("Oh no")

        val configurationService = createConfigurationService(
          projectConfigurationRepository = ProjectConfigurationRepositoryTestImpl.withException(error)
        )
        configurationService.getLastByProjectId(projectId, userId).failed.map(_ shouldBe error)
      }
    }

    "get configuration by project id and user is not project owner" should {
      "return failure" in {
        val error = AccessDenied()

        val configurationService = createConfigurationService(
          projectService = ProjectServiceTestImpl.withException(error)
        )
        configurationService.getLastByProjectId(projectId, strangerId).failed.map(_ shouldBe error)
      }
    }

    "deactivate configuration and user is project owner" should {
      "return complete status for deactivating configuration" in {
        val result: Unit = ()

        configurationService.deactivateLastByProjectId(projectId, userId).map(_ shouldBe result)
      }

      "return exception if no configuration was matched" in {
        val configurationService = createConfigurationService(
          projectConfigurationRepository = ProjectConfigurationRepositoryTestImpl()
        )
        configurationService
          .deactivateLastByProjectId(projectId, userId)
          .failed
          .map(_ should have.message("There is no project to deactivate"))
      }
    }

    "deactivate configuration and user is not project owner" should {
      val error = AccessDenied()

      "return exception" in {
        val configurationService = createConfigurationService(
          projectService = ProjectServiceTestImpl.withException(error)
        )
        configurationService
          .deactivateLastByProjectId(projectId, strangerId)
          .failed
          .map(_ should have.message("Access denied. You are not the project owner"))
      }
    }
    "build configuration" should {

      "return success message for request" taggedAs Service in {
        val configurationService = createConfigurationService(
          projectConfigurationRepository = ProjectConfigurationRepositoryTestImpl()
        )

        configurationService
          .buildConfiguration(projectId, projectFile.path, optionVersion, userId)
          .map(
            builtConfiguration =>
              builtConfiguration shouldBe ProjectConfiguration(
                id = builtConfiguration.id,
                projectId = projectId,
                active = true,
                wdlParams = WdlParams(projectFile.path, Nil),
                version = ProjectConfigurationVersion.defaultVersion
              )
          )
      }
      "return error message for invalid file request" taggedAs Service in {
        val configurationService =
          createConfigurationService(womTool = WomToolTestImpl.withErrorMessages(List(errorMessage)))

        configurationService
          .buildConfiguration(projectId, projectFile.path, optionVersion, userId)
          .failed
          .map(_ shouldBe ValidationError(List(errorMessage)))
      }

      "return error message for error request" taggedAs Service in {
        val configurationService = createConfigurationService(
          projectVersioning = ProjectVersioningTestImpl.withException(VersioningException.FileException("404"))
        )
        configurationService
          .buildConfiguration(projectId, projectFile.path, optionVersion, userId)
          .failed
          .map(_ shouldBe VersioningException.FileException("404"))
      }
    }
  }
}
