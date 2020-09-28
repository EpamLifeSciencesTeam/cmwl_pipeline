package cromwell.pipeline.service

import java.nio.file.Paths

import com.mongodb.client.result.UpdateResult
import cromwell.pipeline.datastorage.dao.repository.DocumentRepository
import cromwell.pipeline.datastorage.dao.repository.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto.project.configuration.ProjectConfigurationEntity
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
      List(
        ProjectFileConfiguration(Paths.get("/home/file"), List(FileParameter("nodeName", StringTyped(Some("hello")))))
      )
    )
    val configurationRequest =
      ProjectConfigurationEntity(configuration.projectId, configuration.projectFileConfigurations)
    val configurationResponse =
      ProjectConfigurationEntity(configuration.projectId, configuration.projectFileConfigurations)
    val document = ProjectConfiguration.toDocument(configuration)
    val updateResult = mock[UpdateResult]
    val projectId = configuration.projectId

    "add configuration" should {
      "return complete status for creating configuration" in {
        when(updateResult.toString).thenReturn("Success update")
        when(configurationRepository.replaceOne(document, "projectId", configuration.projectId.value))
          .thenReturn(Future.successful(updateResult))
        configurationService.addConfiguration(configurationRequest).map(_ shouldBe "Success update")
      }
    }

    "get configuration by project id" should {
      "return nodes by project id" in {
        when(configurationRepository.getByParam("projectId", projectId.value))
          .thenReturn(Future.successful(Some(document)))
        configurationService.getById(projectId).map(_ shouldBe Some(configurationResponse))
      }

      "return None if no configuration was matched" in {
        when(configurationRepository.getByParam("projectId", projectId.value)).thenReturn(Future.successful(None))
        configurationService.getById(projectId).map(_ shouldBe None)
      }
    }

    "deactivate configuration" should {
      "return complete status for deactivated configuration" in {
        when(updateResult.toString).thenReturn("Deactivation success")
        when(configurationRepository.getByParam("projectId", projectId.value))
          .thenReturn(Future.successful(Some(document)))
        when(configurationRepository.updateOneField("projectId", projectId.value, "isActive", false))
          .thenReturn(Future.successful(updateResult))
        configurationService.deactivateConfiguration(configuration.projectId).map(_ shouldBe "Deactivation success")
      }
    }
  }
}
