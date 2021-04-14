package cromwell.pipeline.service

import java.nio.file.Paths

import com.mongodb.client.result.UpdateResult
import cromwell.pipeline.datastorage.dao.repository.DocumentRepository
import cromwell.pipeline.datastorage.dao.repository.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto.{ FileParameter, ProjectConfiguration, ProjectFileConfiguration, StringTyped }
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class ProjectConfigurationServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {
  private val configurationRepository = mock[DocumentRepository]
  private val configurationService = new ProjectConfigurationService(configurationRepository)

  "ConfigurationServiceTest" when {
    val configuration = ProjectConfiguration(
      TestProjectUtils.getDummyProjectId,
      active = true,
      List(
        ProjectFileConfiguration(Paths.get("/home/file"), List(FileParameter("nodeName", StringTyped(Some("hello")))))
      )
    )
    val activeDocument = ProjectConfiguration.toDocument(configuration)
    val inactiveDocument = ProjectConfiguration.toDocument(configuration.copy(active = false))
    val updateResult = mock[UpdateResult]

    "add configuration" should {
      "return complete status for creating configuration" in {
        when(updateResult.toString).thenReturn("Success update")
        when(configurationRepository.updateOne(activeDocument, "projectId", configuration.projectId.value))
          .thenReturn(Future.successful(updateResult))
        configurationService.addConfiguration(configuration).map(_ shouldBe "Success update")
      }
    }

    "get configuration by project id" should {
      val projectId = configuration.projectId

      "return active nodes by project id" in {
        when(configurationRepository.getByParam("projectId", projectId.value))
          .thenReturn(Future.successful(List(activeDocument)))
        configurationService.getById(projectId).map(_ shouldBe Some(configuration))
      }

      "not return inactive nodes by project id" in {
        when(configurationRepository.getByParam("projectId", projectId.value))
          .thenReturn(Future.successful(List(inactiveDocument)))
        configurationService.getById(projectId).map(_ shouldBe None)
      }

      "return None if no configuration was matched" in {
        when(configurationRepository.getByParam("projectId", projectId.value)).thenReturn(Future.successful(List()))
        configurationService.getById(projectId).map(_ shouldBe None)
      }
    }

    "deactivate configuration" should {
      val projectId = configuration.projectId

      "return complete status for deactivating configuration" in {
        when(configurationRepository.getByParam("projectId", projectId.value))
          .thenReturn(Future.successful(List(activeDocument)))

        when(updateResult.toString).thenReturn("Success update")
        when(configurationRepository.updateOne(inactiveDocument, "projectId", projectId.value))
          .thenReturn(Future.successful(updateResult))
        configurationService.deactivateConfiguration(projectId).map(_ shouldBe "Success update")
      }

      "return exception if no configuration was matched" in {
        when(configurationRepository.getByParam("projectId", projectId.value)).thenReturn(Future.successful(List()))

        configurationService
          .deactivateConfiguration(projectId)
          .failed
          .map(_ should have.message("There is no project to deactivate"))
      }
    }
  }
}
