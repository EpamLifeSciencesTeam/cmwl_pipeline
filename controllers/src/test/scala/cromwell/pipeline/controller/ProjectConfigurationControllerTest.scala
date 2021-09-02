package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dao.utils.{ TestProjectUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.service.ProjectService.Exceptions.ProjectNotFoundException
import cromwell.pipeline.service.{ ProjectConfigurationService, VersioningException }
import cromwell.pipeline.utils.URLEncoderUtils
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import java.nio.file.Paths
import scala.concurrent.Future

class ProjectConfigurationControllerTest extends AsyncWordSpec with Matchers with ScalatestRouteTest with MockitoSugar {
  private val configurationService = mock[ProjectConfigurationService]
  private val configurationController = new ProjectConfigurationController(configurationService)

  "ProjectConfigurationController" when {
    val accessToken = AccessTokenContent(TestUserUtils.getDummyUserId)
    val projectId = TestProjectUtils.getDummyProjectId
    val configuration = ProjectConfiguration(
      ProjectConfigurationId.randomId,
      projectId,
      active = true,
      WdlParams(Paths.get("/home/file"), List(FileParameter("nodeName", StringTyped(Some("hello"))))),
      ProjectConfigurationVersion.defaultVersion
    )
    val configurationAdditionRequest = ProjectConfigurationAdditionRequest(
      id = configuration.id,
      active = configuration.active,
      wdlParams = configuration.wdlParams,
      version = configuration.version
    )
    val versionString = "v0.0.2"
    val version = PipelineVersion(versionString)
    val versionOption = Some(version)
    val path = Paths.get("folder/file.wdl")
    val pathString = URLEncoderUtils.encode(path.toString)

    "update configuration" should {
      val error = new RuntimeException("Something went wrong")

      "return success for update configuration" in {
        when(configurationService.addConfiguration(configuration, accessToken.userId)).thenReturn(Future.unit)
        Put(s"/projects/${projectId.value}/configurations", configurationAdditionRequest) ~> configurationController
          .route(
            accessToken
          ) ~> check {
          status shouldBe StatusCodes.OK
        }
      }

      "return InternalServerError when failure update configuration" in {
        when(configurationService.addConfiguration(configuration, accessToken.userId)).thenReturn(Future.failed(error))
        Put(s"/projects/${projectId.value}/configurations", configurationAdditionRequest) ~> configurationController
          .route(
            accessToken
          ) ~> check {
          status shouldBe StatusCodes.InternalServerError
          entityAs[String] shouldBe "Something went wrong"
        }
      }

      "return NotFound when failure find project to update configuration" in {
        when(configurationService.addConfiguration(configuration, accessToken.userId))
          .thenReturn(Future.failed(new ProjectNotFoundException))
        Put(s"/projects/${projectId.value}/configurations", configurationAdditionRequest) ~> configurationController
          .route(
            accessToken
          ) ~> check {
          status shouldBe StatusCodes.NotFound
        }
      }
    }

    "get configuration by project id" should {
      "return configuration by existing project id" in {
        when(configurationService.getLastByProjectId(projectId, accessToken.userId))
          .thenReturn(Future.successful(Some(configuration)))
        Get(s"/projects/${projectId.value}/configurations") ~> configurationController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          entityAs[ProjectConfiguration] shouldBe configuration
        }
      }

      "return message about no project with this project id" in {
        when(configurationService.getLastByProjectId(projectId, accessToken.userId)).thenReturn(Future.successful(None))
        Get(s"/projects/${projectId.value}/configurations") ~> configurationController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NotFound
          entityAs[String] shouldBe s"There is no configuration with project_id: ${projectId.value}"
        }
      }
    }

    "deactivate configuration" should {
      val error = new RuntimeException("Something went wrong")

      "return success for deactivate configuration" in {
        when(configurationService.deactivateLastByProjectId(projectId, accessToken.userId)).thenReturn(Future.unit)
        Delete(s"/projects/${projectId.value}/configurations") ~> configurationController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NoContent
        }
      }

      "return InternalServerError when failure deactivate configuration" in {
        when(configurationService.deactivateLastByProjectId(projectId, accessToken.userId))
          .thenReturn(Future.failed(error))
        Delete(s"/projects/${projectId.value}/configurations") ~> configurationController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
          entityAs[String] shouldBe "Something went wrong"
        }
      }

      "return NotFound when failure find project to deactivate configuration" in {
        when(configurationService.deactivateLastByProjectId(projectId, accessToken.userId))
          .thenReturn(Future.failed(new ProjectNotFoundException))
        Delete(s"/projects/${projectId.value}/configurations") ~> configurationController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NotFound
        }
      }
    }

    "build configuration" should {

      "return configuration for file" in {
        when(configurationService.buildConfiguration(projectId, path, versionOption, accessToken.userId))
          .thenReturn(Future.successful(configuration))
        Get(s"/projects/${projectId.value}/configurations/files/$pathString?version=$versionString") ~>
        configurationController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          entityAs[ProjectConfiguration] shouldBe configuration
        }
      }

      "return failed for Bad request" in {
        when(configurationService.buildConfiguration(projectId, path, versionOption, accessToken.userId))
          .thenReturn(Future.failed(VersioningException.HttpException("Bad request")))
        Get(s"/projects/${projectId.value}/configurations/files/$pathString?version=$versionString") ~>
        configurationController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
          entityAs[String] shouldBe "Bad request"
        }
      }

      "return failed for invalid file" in {
        when(configurationService.buildConfiguration(projectId, path, versionOption, accessToken.userId))
          .thenReturn(Future.failed(ValidationError(List("invalid some field"))))
        Get(s"/projects/${projectId.value}/configurations/files/$pathString?version=$versionString") ~>
        configurationController.route(accessToken) ~> check {
          status shouldBe StatusCodes.UnprocessableEntity
          entityAs[List[String]] shouldBe List("invalid some field")
        }
      }
    }
  }
}
