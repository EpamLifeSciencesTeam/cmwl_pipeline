package cromwell.pipeline.controller

import java.nio.file.Paths

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dao.repository.utils.{ TestProjectUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.datastorage.dto.project.configuration.ProjectConfigurationEntity
import cromwell.pipeline.service.ProjectConfigurationService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class ProjectConfigurationControllerTest extends AsyncWordSpec with Matchers with ScalatestRouteTest with MockitoSugar {
  private val configurationService = mock[ProjectConfigurationService]
  private val configurationController = new ProjectConfigurationController(configurationService)

  "ProjectConfigurationController" when {
    val accessToken = AccessTokenContent(TestUserUtils.getDummyUserId)
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
    val error = new RuntimeException("Something went wrong")

    "update configuration" should {

      "return success for update configuration" in {
        when(configurationService.addConfiguration(configurationRequest)).thenReturn(Future.successful("Success"))
        Put("/configurations", configuration) ~> configurationController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          entityAs[String] shouldBe "Success"
        }
      }

      "return InternalServerError when failure update configuration" in {
        when(configurationService.addConfiguration(configurationRequest)).thenReturn(Future.failed(error))
        Put("/configurations", configuration) ~> configurationController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
          entityAs[String] shouldBe "Something went wrong"
        }
      }
    }

    "get configuration by project id" should {
      val projectId = TestProjectUtils.getDummyProjectId

      "return configuration by existing project id" in {
        when(configurationService.getById(projectId)).thenReturn(Future.successful(Some(configurationResponse)))
        Get("/configurations?project_id=" + projectId.value) ~> configurationController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          entityAs[ProjectConfigurationEntity] shouldBe configurationResponse
        }
      }

      "return message about no project with this project id" in {
        when(configurationService.getById(projectId)).thenReturn(Future.successful(None))
        Get("/configurations?project_id=" + projectId.value) ~> configurationController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NotFound
          entityAs[String] shouldBe s"There is no configuration with project_id: ${projectId.value}"
        }
      }
    }

    "deactivate configuration" should {

      "return success for deactivation configuration" in {
        when(configurationService.deactivateConfiguration(configuration.projectId))
          .thenReturn(Future.successful("Success"))
        Delete("/configurations?project_id=" + configuration.projectId.value) ~> configurationController.route(
          accessToken
        ) ~> check {
          status shouldBe StatusCodes.OK
          entityAs[String] shouldBe "Success"
        }
      }

      "return InternalServerError when failure deactivation configuration" in {
        when(configurationService.deactivateConfiguration(configuration.projectId)).thenReturn(Future.failed(error))
        Delete("/configurations?project_id=" + configuration.projectId.value) ~> configurationController.route(
          accessToken
        ) ~> check {
          status shouldBe StatusCodes.InternalServerError
          entityAs[String] shouldBe "Something went wrong"
        }
      }
    }
  }
}
