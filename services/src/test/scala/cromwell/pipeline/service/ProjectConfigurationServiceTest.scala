package cromwell.pipeline.service

import java.nio.file.Paths
import cromwell.pipeline.datastorage.dao.repository.ProjectConfigurationRepository
import cromwell.pipeline.datastorage.dao.utils.{ TestProjectUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.service.Exceptions.ProjectAccessDeniedException
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class ProjectConfigurationServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {
  private val projectService = mock[ProjectService]
  private val configurationRepository = mock[ProjectConfigurationRepository]
  private val dummyProject: Project = TestProjectUtils.getDummyProject()
  private val configurationService = new ProjectConfigurationService(configurationRepository, projectService)
  private val projectId: ProjectId = dummyProject.projectId
  private val userId: UserId = dummyProject.ownerId
  private val strangerId: UserId = TestUserUtils.getDummyUserId

  private val projectFileConfiguration: ProjectFileConfiguration =
    ProjectFileConfiguration(Paths.get("/home/file"), List(FileParameter("nodeName", StringTyped(Some("hello")))))

  private val activeConfiguration: ProjectConfiguration =
    ProjectConfiguration(
      projectId = projectId,
      active = true,
      projectFileConfigurations = List(projectFileConfiguration)
    )

  private val inactiveConfiguration: ProjectConfiguration =
    activeConfiguration.copy(active = false)

  private val updatedConfiguration: ProjectConfiguration = {
    val updatedProjectFileConfiguration =
      projectFileConfiguration.copy(inputs = List(FileParameter("nodeName", StringTyped(Some("hi")))))
    activeConfiguration.copy(projectFileConfigurations = List(updatedProjectFileConfiguration))
  }

  "ProjectConfigurationService" when {
    "add new configuration for project and user is project owner" should {
      val result: Unit = ()
      val error = new Exception("Oh no")
      when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(dummyProject))

      "return success if creation was successful" in {
        when(configurationRepository.getById(projectId)).thenReturn(Future.successful(None))
        when(configurationRepository.addConfiguration(activeConfiguration)).thenReturn(Future.successful(result))
        configurationService.addConfiguration(activeConfiguration, userId).map(_ shouldBe result)
      }

      "return success if update was successful" in {
        when(configurationRepository.getById(projectId)).thenReturn(Future.successful(Some(activeConfiguration)))
        when(configurationRepository.updateConfiguration(updatedConfiguration)).thenReturn(Future.successful(result))
        configurationService.addConfiguration(updatedConfiguration, userId).map(_ shouldBe result)
      }

      "return failure if it couldn't fetch existing configuration" in {
        when(configurationRepository.getById(projectId)).thenReturn(Future.failed(error))
        configurationService.addConfiguration(activeConfiguration, userId).failed.map(_ shouldBe error)
      }

      "return failure if creation wasn't successful" in {
        when(configurationRepository.getById(projectId)).thenReturn(Future.successful(None))
        when(configurationRepository.addConfiguration(activeConfiguration)).thenReturn(Future.failed(error))
        configurationService.addConfiguration(activeConfiguration, userId).failed.map(_ shouldBe error)
      }

      "return failure if update wasn't successful" in {
        when(configurationRepository.getById(projectId)).thenReturn(Future.successful(Some(activeConfiguration)))
        when(configurationRepository.updateConfiguration(activeConfiguration)).thenReturn(Future.failed(error))
        configurationService.addConfiguration(activeConfiguration, userId).failed.map(_ shouldBe error)
      }
    }

    "add new configuration for project and user is not project owner" should {
      val error = new ProjectAccessDeniedException
      when(projectService.getUserProjectById(projectId, strangerId)).thenReturn(Future.failed(error))

      "return failure" in {
        when(configurationRepository.getById(projectId)).thenReturn(Future.successful(Some(activeConfiguration)))
        configurationService.addConfiguration(activeConfiguration, strangerId).failed.map(_ shouldBe error)
      }
    }

    "get configuration by project id and user is project owner" should {
      when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(dummyProject))

      "return project if it was found" in {
        val result: Option[ProjectConfiguration] = Some(activeConfiguration)
        when(configurationRepository.getById(projectId)).thenReturn(Future.successful(result))
        configurationService.getConfigurationById(projectId, userId).map(_ shouldBe result)
      }

      "not return inactive project" in {
        val result: Option[ProjectConfiguration] = Some(inactiveConfiguration)
        when(configurationRepository.getById(projectId)).thenReturn(Future.successful(result))
        configurationService.getConfigurationById(projectId, userId).map(_ shouldBe None)
      }

      "not fail if project wasn't found" in {
        val result: Option[ProjectConfiguration] = None
        when(configurationRepository.getById(projectId)).thenReturn(Future.successful(result))
        configurationService.getConfigurationById(projectId, userId).map(_ shouldBe result)
      }

      "return failure if repository returned error" in {
        val error = new Exception("Oh no")
        when(configurationRepository.getById(projectId)).thenReturn(Future.failed(error))
        configurationService.getConfigurationById(projectId, userId).failed.map(_ shouldBe error)
      }
    }

    "get configuration by project id and user is not project owner" should {
      val error = new ProjectAccessDeniedException
      when(projectService.getUserProjectById(projectId, strangerId)).thenReturn(Future.failed(error))
      "return failure" in {
        configurationService.getConfigurationById(projectId, strangerId).failed.map(_ shouldBe error)
      }
    }

    "deactivate configuration and user is project owner" should {
      when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(dummyProject))
      "return complete status for deactivating configuration" in {
        val result: Unit = ()
        when(configurationRepository.getById(projectId)).thenReturn(Future.successful(Some(activeConfiguration)))
        when(configurationRepository.updateConfiguration(inactiveConfiguration)).thenReturn(Future.successful(result))
        configurationService.deactivateConfiguration(projectId, userId).map(_ shouldBe result)
      }

      "return exception if no configuration was matched" in {
        when(configurationRepository.getById(projectId)).thenReturn(Future.successful(None))
        configurationService
          .deactivateConfiguration(projectId, userId)
          .failed
          .map(_ should have.message("There is no project to deactivate"))
      }
    }

    "deactivate configuration and user is not project owner" should {
      val error = new ProjectAccessDeniedException
      when(projectService.getUserProjectById(projectId, strangerId)).thenReturn(Future.failed(error))

      "return exception" in {
        configurationService
          .deactivateConfiguration(projectId, strangerId)
          .failed
          .map(_ should have.message("Access denied. You  not owner of the project"))
      }
    }
  }
}
