package cromwell.pipeline.service

import cats.data.NonEmptyList
import cromwell.pipeline.datastorage.dao.repository.ProjectConfigurationRepository
import cromwell.pipeline.datastorage.dao.utils.{ TestProjectUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.service.ProjectService.Exceptions.ProjectAccessDeniedException
import cromwell.pipeline.womtool.WomTool
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import java.nio.file.{ Path, Paths }
import scala.concurrent.Future

class ProjectConfigurationServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {
  private val projectService = mock[ProjectService]
  private val configurationRepository = mock[ProjectConfigurationRepository]
  private val womTool = mock[WomTool]
  private val projectVersioning = mock[ProjectVersioning[VersioningException]]
  private val dummyProject: Project = TestProjectUtils.getDummyProject()
  private val configurationService =
    ProjectConfigurationService(configurationRepository, projectService, womTool, projectVersioning)
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
    "add new configuration for project and user is project owner" should {
      val result: Unit = ()
      val error = new Exception("Oh no")
      when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(dummyProject))

      "return success if creation was successful" in {
        when(configurationRepository.addConfiguration(activeConfiguration)).thenReturn(Future.successful(result))
        configurationService.addConfiguration(activeConfiguration, userId).map(_ shouldBe result)
      }

      "return failure if creation wasn't successful" in {
        when(configurationRepository.addConfiguration(activeConfiguration)).thenReturn(Future.failed(error))
        configurationService.addConfiguration(activeConfiguration, userId).failed.map(_ shouldBe error)
      }
    }

    "add new configuration for project and user is not project owner" should {
      val error = new ProjectAccessDeniedException
      when(projectService.getUserProjectById(projectId, strangerId)).thenReturn(Future.failed(error))

      "return failure" in {
        configurationService.addConfiguration(activeConfiguration, strangerId).failed.map(_ shouldBe error)
      }
    }

    "get configuration by project id and user is project owner" should {
      when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(dummyProject))

      "return project if it was found" in {
        val result: Option[ProjectConfiguration] = Some(activeConfiguration)
        when(configurationRepository.getAllByProjectId(projectId))
          .thenReturn(Future.successful(Seq(activeConfiguration)))
        configurationService.getLastByProjectId(projectId, userId).map(_ shouldBe result)
      }

      "not return inactive project" in {
        val result: Seq[ProjectConfiguration] = Seq(inactiveConfiguration)
        when(configurationRepository.getAllByProjectId(projectId)).thenReturn(Future.successful(result))
        configurationService.getLastByProjectId(projectId, userId).map(_ shouldBe None)
      }

      "not fail if project wasn't found" in {
        val result: Option[ProjectConfiguration] = None
        when(configurationRepository.getAllByProjectId(projectId)).thenReturn(Future.successful(Seq.empty))
        configurationService.getLastByProjectId(projectId, userId).map(_ shouldBe result)
      }

      "return failure if repository returned error" in {
        val error = new Exception("Oh no")
        when(configurationRepository.getAllByProjectId(projectId)).thenReturn(Future.failed(error))
        configurationService.getLastByProjectId(projectId, userId).failed.map(_ shouldBe error)
      }
    }

    "get configuration by project id and user is not project owner" should {
      val error = new ProjectAccessDeniedException
      when(projectService.getUserProjectById(projectId, strangerId)).thenReturn(Future.failed(error))
      "return failure" in {
        configurationService.getLastByProjectId(projectId, strangerId).failed.map(_ shouldBe error)
      }
    }

    "deactivate configuration and user is project owner" should {
      when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(dummyProject))
      "return complete status for deactivating configuration" in {
        val result: Unit = ()
        when(configurationRepository.getAllByProjectId(projectId))
          .thenReturn(Future.successful(Seq(activeConfiguration)))
        when(configurationRepository.updateConfiguration(inactiveConfiguration)).thenReturn(Future.successful(result))
        configurationService.deactivateLastByProjectId(projectId, userId).map(_ shouldBe result)
      }

      "return exception if no configuration was matched" in {
        when(configurationRepository.getAllByProjectId(projectId)).thenReturn(Future.successful(Seq.empty))
        configurationService
          .deactivateLastByProjectId(projectId, userId)
          .failed
          .map(_ should have.message("There is no project to deactivate"))
      }
    }

    "deactivate configuration and user is not project owner" should {
      val error = new ProjectAccessDeniedException
      when(projectService.getUserProjectById(projectId, strangerId)).thenReturn(Future.failed(error))

      "return exception" in {
        configurationService
          .deactivateLastByProjectId(projectId, strangerId)
          .failed
          .map(_ should have.message("Access denied. You  not owner of the project"))
      }
    }
    "build configuration" should {

      "return success message for request" taggedAs Service in {

        when(configurationRepository.getAllByProjectId(projectId)).thenReturn(Future.successful(Seq()))
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(dummyProject))
        when(projectVersioning.getFile(dummyProject, projectFile.path, optionVersion))
          .thenReturn(Future.successful(Right(projectFile)))
        when(womTool.inputsToList(projectFileContent.content)).thenReturn(Right(Nil))

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
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(dummyProject))
        when(projectVersioning.getFile(dummyProject, projectFile.path, optionVersion))
          .thenReturn(Future.successful(Right(projectFile)))
        when(womTool.inputsToList(projectFileContent.content)).thenReturn(Left(NonEmptyList(errorMessage, Nil)))

        configurationService
          .buildConfiguration(projectId, projectFile.path, optionVersion, userId)
          .failed
          .map(_ shouldBe ValidationError(List(errorMessage)))
      }

      "return error message for error request" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(dummyProject))
        when(projectVersioning.getFile(dummyProject, projectFile.path, optionVersion))
          .thenReturn(Future.successful(Left(VersioningException.FileException("404"))))

        configurationService
          .buildConfiguration(projectId, projectFile.path, optionVersion, userId)
          .failed
          .map(_ shouldBe VersioningException.FileException("404"))
      }
    }
  }
}
