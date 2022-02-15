package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.ProjectConfigurationRepository
import cromwell.pipeline.datastorage.dao.repository.impls.ProjectConfigurationRepositoryTestImpl
import cromwell.pipeline.datastorage.dao.utils.{ TestProjectUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.validator.Enable
import cromwell.pipeline.model.wrapper.{ ProjectConfigurationId, ProjectId, UserId }
import cromwell.pipeline.service.ProjectConfigurationService.Exceptions._
import cromwell.pipeline.service.impls.{ ProjectServiceTestImpl, ProjectVersioningTestImpl, WomToolTestImpl }
import cromwell.pipeline.womtool.WomToolAPI
import org.scalatest.{ AsyncWordSpec, Matchers }

import java.nio.file.{ Path, Paths }
import java.util.UUID

class ProjectConfigurationServiceTest extends AsyncWordSpec with Matchers {

  private val dummyProject: Project = TestProjectUtils.getDummyProject()
  private val projectId: ProjectId = dummyProject.projectId
  private val userId: UserId = dummyProject.ownerId
  private val strangerId: UserId = TestUserUtils.getDummyUserId
  private val projectConfigurationId = ProjectConfigurationId(UUID.randomUUID().toString, Enable.Unsafe)
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

      "return success if creation was successful" in {
        configurationService.addConfiguration(activeConfiguration, userId).map(_ shouldBe result)
      }

      "return failure if creation wasn't successful" in {
        val error = InternalError("Failed to add configuration due to unexpected internal error")
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
        val result: ProjectConfiguration = activeConfiguration

        configurationService.getLastByProjectId(projectId, userId).map(_ shouldBe result)
      }

      "not return inactive project" in {
        val result: Seq[ProjectConfiguration] = Seq(inactiveConfiguration)
        val error = NotFound(
          s"There is no configuration with project_id: ${projectId.value}"
        )
        val configurationService = createConfigurationService(
          projectConfigurationRepository = ProjectConfigurationRepositoryTestImpl(result: _*)
        )

        configurationService.getLastByProjectId(projectId, userId).failed.map(_ shouldBe error)
      }

      "fail if project wasn't found" in {
        val error = NotFound(
          s"There is no configuration with project_id: ${projectId.value}"
        )
        val configurationService = createConfigurationService(
          projectConfigurationRepository = ProjectConfigurationRepositoryTestImpl()
        )

        configurationService.getLastByProjectId(projectId, userId).failed.map(_ shouldBe error)
      }

      "return failure if repository returned error" in {
        val error = InternalError(
          s"Failed to find configuration due to unexpected internal error"
        )

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
          .map(_ should have.message(s"There is no configuration with project_id: ${projectId.value}"))
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
        val error = ProjectConfigurationService.Exceptions.ValidationError(errorMessage)
        configurationService
          .buildConfiguration(projectId, projectFile.path, optionVersion, userId)
          .failed
          .map(_ shouldBe error)
      }

      "return error message for error request" taggedAs Service in {
        val configurationService = createConfigurationService(
          projectVersioning = ProjectVersioningTestImpl.withException(VersioningException.FileException("404"))
        )
        val error =
          ProjectConfigurationService.Exceptions.InternalError("Failed to get file due to unexpected internal error")

        configurationService
          .buildConfiguration(projectId, projectFile.path, optionVersion, userId)
          .failed
          .map(_ shouldBe error)
      }
    }
  }
}
