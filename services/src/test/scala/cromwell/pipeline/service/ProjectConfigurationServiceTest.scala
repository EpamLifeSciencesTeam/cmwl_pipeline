package cromwell.pipeline.service

import java.nio.file.Paths

import cromwell.pipeline.datastorage.dao.repository.ProjectConfigurationRepository
import cromwell.pipeline.datastorage.dao.repository.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto._
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class ProjectConfigurationServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {
  private val configurationRepository = mock[ProjectConfigurationRepository]
  private val configurationService = new ProjectConfigurationService(configurationRepository)

  private val projectFileConfiguration: ProjectFileConfiguration =
    ProjectFileConfiguration(Paths.get("/home/file"), List(FileParameter("nodeName", StringTyped(Some("hello")))))
  private val projectId: ProjectId = TestProjectUtils.getDummyProjectId

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
    "add new configuration for project" should {
      "return success if creation was successful" in {
        val result: Unit = ()
        when(configurationRepository.getById(projectId)).thenReturn(Future.successful(None))
        when(configurationRepository.addConfiguration(activeConfiguration)).thenReturn(Future.successful(result))
        configurationService.addConfiguration(activeConfiguration).map(_ shouldBe result)
      }

      "return success if update was successful" in {
        val result: Unit = ()
        when(configurationRepository.getById(projectId)).thenReturn(Future.successful(Some(activeConfiguration)))
        when(configurationRepository.updateConfiguration(updatedConfiguration)).thenReturn(Future.successful(result))
        configurationService.addConfiguration(updatedConfiguration).map(_ shouldBe result)
      }

      "return failure if it couldn't fetch existing configuration" in {
        val error = new Exception("Oh no")
        when(configurationRepository.getById(projectId)).thenReturn(Future.failed(error))
        configurationService.addConfiguration(activeConfiguration).failed.map(_ shouldBe error)
      }

      "return failure if creation wasn't successful" in {
        val error = new Exception("Oh no")
        when(configurationRepository.getById(projectId)).thenReturn(Future.successful(None))
        when(configurationRepository.addConfiguration(activeConfiguration)).thenReturn(Future.failed(error))
        configurationService.addConfiguration(activeConfiguration).failed.map(_ shouldBe error)
      }

      "return failure if update wasn't successful" in {
        val error = new Exception("Oh no")
        when(configurationRepository.getById(projectId)).thenReturn(Future.successful(Some(activeConfiguration)))
        when(configurationRepository.updateConfiguration(activeConfiguration)).thenReturn(Future.failed(error))
        configurationService.addConfiguration(activeConfiguration).failed.map(_ shouldBe error)
      }
    }
    "get configuration by project id" should {
      "return project if it was found" in {
        val result: Option[ProjectConfiguration] = Some(activeConfiguration)
        when(configurationRepository.getById(projectId)).thenReturn(Future.successful(result))
        configurationService.getById(projectId).map(_ shouldBe result)
      }

      "not return inactive project" in {
        val result: Option[ProjectConfiguration] = Some(inactiveConfiguration)
        when(configurationRepository.getById(projectId)).thenReturn(Future.successful(result))
        configurationService.getById(projectId).map(_ shouldBe None)
      }

      "not fail if project wasn't found" in {
        val result: Option[ProjectConfiguration] = None
        when(configurationRepository.getById(projectId)).thenReturn(Future.successful(result))
        configurationService.getById(projectId).map(_ shouldBe result)
      }

      "return failure if repository returned error" in {
        val error = new Exception("Oh no")
        when(configurationRepository.getById(projectId)).thenReturn(Future.failed(error))
        configurationService.getById(projectId).failed.map(_ shouldBe error)
      }
    }

    "deactivate configuration" should {
      val projectId = activeConfiguration.projectId
      val updateResult: Unit = ()

      "return complete status for deactivating configuration" in {
        when(configurationRepository.getById(projectId)).thenReturn(Future.successful(Some(activeConfiguration)))
        when(configurationRepository.updateConfiguration(inactiveConfiguration))
          .thenReturn(Future.successful(updateResult))
        configurationService.deactivateConfiguration(projectId).map(_ shouldBe updateResult)
      }

      "return exception if no configuration was matched" in {
        when(configurationRepository.getById(projectId)).thenReturn(Future.successful(None))
        configurationService
          .deactivateConfiguration(projectId)
          .failed
          .map(_ should have.message("There is no project to deactivate"))
      }
    }
  }
}
