package cromwell.pipeline.datastorage.dao.repository

import java.nio.file.Paths

import cromwell.pipeline.datastorage.dao.mongo.DocumentCodecInstances.projectConfigurationDocumentCodec
import cromwell.pipeline.datastorage.dao.mongo.DocumentRepository
import cromwell.pipeline.datastorage.dao.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto._
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class ProjectConfigurationRepositoryTest extends AsyncWordSpec with Matchers with MockitoSugar {

  private val documentRepository = mock[DocumentRepository]
  private val configurationRepository = new ProjectConfigurationRepository(documentRepository)

  private val projectFileConfiguration: ProjectFileConfiguration =
    ProjectFileConfiguration(Paths.get("/home/file"), List(FileParameter("nodeName", StringTyped(Some("hello")))))
  private val projectId: ProjectId = TestProjectUtils.getDummyProjectId

  private val configuration: ProjectConfiguration =
    ProjectConfiguration(
      projectId = projectId,
      active = true,
      projectFileConfigurations = List(projectFileConfiguration)
    )

  "ProjectConfigurationRepository" when {
    "add configuration for project" should {
      "return success if creation was successful" in {
        val result: Unit = ()
        when(documentRepository.upsertOne(configuration, "projectId", projectId.value))
          .thenReturn(Future.successful(result))
        configurationRepository.addConfiguration(configuration).map(_ shouldBe result)
      }
      "return failure if creation wasn't successful" in {
        val error = new Exception("Oh no")
        when(documentRepository.upsertOne(configuration, "projectId", projectId.value)).thenReturn(Future.failed(error))
        configurationRepository.addConfiguration(configuration).failed.map(_ shouldBe error)
      }
    }

    "update configuration for project" should {
      "return success if update was successful" in {
        val result: Unit = ()
        when(documentRepository.upsertOne(configuration, "projectId", projectId.value))
          .thenReturn(Future.successful(result))
        configurationRepository.updateConfiguration(configuration).map(_ shouldBe result)
      }
      "return failure if update wasn't successful" in {
        val error = new Exception("Oh no")
        when(documentRepository.upsertOne(configuration, "projectId", projectId.value)).thenReturn(Future.failed(error))
        configurationRepository.updateConfiguration(configuration).failed.map(_ shouldBe error)
      }
    }

    "get configuration by project id" should {
      "return configuration by project id" in {
        when(documentRepository.getByParam("projectId", projectId.value))
          .thenReturn(Future.successful(List(configuration)))
        configurationRepository.getById(projectId).map(_ shouldBe Some(configuration))
      }

      "return None if no configuration was matched" in {
        when(documentRepository.getByParam("projectId", projectId.value)).thenReturn(Future.successful(List()))
        configurationRepository.getById(projectId).map(_ shouldBe None)
      }
    }
  }

}
