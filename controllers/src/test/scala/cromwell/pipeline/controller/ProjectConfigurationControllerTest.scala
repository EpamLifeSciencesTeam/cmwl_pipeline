package cromwell.pipeline.controller

import java.nio.file.Paths

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dao.utils.{ TestProjectUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
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
      ProjectConfigurationId.randomId,
      TestProjectUtils.getDummyProjectId,
      active = true,
      List(
        ProjectFileConfiguration(Paths.get("/home/file"), List(FileParameter("nodeName", StringTyped(Some("hello")))))
      ),
      ProjectConfigurationVersion.defaultVersion
    )

    "update configuration" should {
      val error = new RuntimeException("Something went wrong")

      "return success for update configuration" in {
        when(configurationService.addConfiguration(configuration, accessToken.userId)).thenReturn(Future.unit)
        Put("/configurations", configuration) ~> configurationController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
        }
      }

      "return InternalServerError when failure update configuration" in {
        when(configurationService.addConfiguration(configuration, accessToken.userId)).thenReturn(Future.failed(error))
        Put("/configurations", configuration) ~> configurationController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
          entityAs[String] shouldBe "Something went wrong"
        }
      }
    }

    "get configuration by project id" should {
      val projectId = TestProjectUtils.getDummyProjectId

      "return configuration by existing project id" in {
        when(configurationService.getLastByProjectId(projectId, accessToken.userId))
          .thenReturn(Future.successful(Some(configuration)))
        Get("/configurations?project_id=" + projectId.value) ~> configurationController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          entityAs[ProjectConfiguration] shouldBe configuration
        }
      }

      "return message about no project with this project id" in {
        when(configurationService.getLastByProjectId(projectId, accessToken.userId)).thenReturn(Future.successful(None))
        Get("/configurations?project_id=" + projectId.value) ~> configurationController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NotFound
          entityAs[String] shouldBe s"There is no configuration with project_id: ${projectId.value}"
        }
      }
    }

    "deactivate configuration" should {
      val error = new RuntimeException("Something went wrong")
      val projectId = TestProjectUtils.getDummyProjectId

      "return success for deactivate configuration" in {
        when(configurationService.deactivateLastByProjectId(projectId, accessToken.userId)).thenReturn(Future.unit)
        Delete("/configurations?project_id=" + projectId.value) ~> configurationController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NoContent
        }
      }

      "return InternalServerError when failure deactivate configuration" in {
        when(configurationService.deactivateLastByProjectId(projectId, accessToken.userId))
          .thenReturn(Future.failed(error))
        Delete("/configurations?project_id=" + projectId.value) ~> configurationController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
          entityAs[String] shouldBe "Something went wrong"
        }
      }
    }
  }
}
