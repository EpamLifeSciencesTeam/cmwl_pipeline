package cromwell.pipeline.controller

import java.nio.file.Paths

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dao.utils.{ TestProjectUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.service.{ ProjectFileService, VersioningException }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class ProjectFileControllerTest extends AsyncWordSpec with Matchers with ScalatestRouteTest with MockitoSugar {
  private val projectFileService: ProjectFileService = mock[ProjectFileService]
  private val projectFileController = new ProjectFileController(projectFileService)

  "ProjectFileController" when {
    val accessToken = AccessTokenContent(TestUserUtils.getDummyUserId)

    "validate file" should {
      val content = ProjectFileContent("task hello {}")
      val request = ValidateFileContentRequest(content)

      "return OK response to valid file" taggedAs Controller in {
        when(projectFileService.validateFile(content)).thenReturn(Future.successful(Right(())))
        Post("/files/validation", request) ~> projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
        }
      }

      "return error response to invalid file" taggedAs Controller in {
        when(projectFileService.validateFile(content))
          .thenReturn(Future.successful(Left(ValidationError(List("Miss close bracket")))))
        Post("/files/validation", request) ~> projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.Conflict
          entityAs[List[String]] shouldBe List("Miss close bracket")
        }
      }
    }

    "upload file" should {
      val version = PipelineVersion("v0.0.2")
      val accessToken = AccessTokenContent(TestUserUtils.getDummyUserId)
      val projectId = TestProjectUtils.getDummyProjectId
      val projectFileContent = ProjectFileContent("file context")
      val projectFile = ProjectFile(Paths.get("folder/test.txt"), projectFileContent)
      val request = ProjectUpdateFileRequest(projectId, projectFile, Some(version))

      "return OK response for valid request with a valid file" taggedAs Controller in {
        when(projectFileService.validateFile(projectFileContent)).thenReturn(Future.successful(Right(())))
        when(projectFileService.uploadFile(projectId, projectFile, Some(version), accessToken.userId))
          .thenReturn(Future.successful(Right(UpdateFiledResponse("test.wdl", "master"))))
        Post("/files", request) ~> projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
        }
      }

      "return Precondition File response for valid request with an invalid file" taggedAs Controller in {
        when(projectFileService.validateFile(projectFileContent))
          .thenReturn(Future.successful(Left(ValidationError(List("Miss close bracket")))))
        when(projectFileService.uploadFile(projectId, projectFile, Some(version), accessToken.userId))
          .thenReturn(Future.successful(Right(UpdateFiledResponse("test.wdl", "master"))))
        Post("/files", request) ~> projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.Created
        }
      }

      "return UnprocessableEntity for bad request" taggedAs Controller in {
        when(projectFileService.uploadFile(projectId, projectFile, Some(version), accessToken.userId))
          .thenReturn(Future.successful(Left(VersioningException.HttpException("Bad request"))))
        Post("/files", request) ~> projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.UnprocessableEntity
          entityAs[String] shouldBe "File have not uploaded due to Bad request"
        }
      }
    }

    "build configuration" should {
      val versionString = "v0.0.2"
      val versionOption = Some(PipelineVersion("v0.0.2"))
      val projectId = TestProjectUtils.getDummyProjectId
      val projectConfigurationId = ProjectConfigurationId.randomId
      val path = "/home/test/file"
      val configuration = ProjectConfiguration(
        projectConfigurationId,
        projectId,
        active = true,
        List(
          ProjectFileConfiguration(
            Paths.get(path),
            List(FileParameter("nodeName", StringTyped(Some("hello"))))
          )
        ),
        ProjectConfigurationVersion.defaultVersion
      )

      "return configuration for file" in {
        when(projectFileService.buildConfiguration(projectId, Paths.get(path), versionOption, accessToken.userId))
          .thenReturn(Future.successful(configuration))
        Get(s"/files/configurations?project_id=${projectId.value}&project_file_path=$path&version=$versionString") ~> projectFileController
          .route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          entityAs[ProjectConfiguration] shouldBe configuration
        }
      }

      "return failed for Bad request" in {
        when(projectFileService.buildConfiguration(projectId, Paths.get(path), versionOption, accessToken.userId))
          .thenReturn(Future.failed(VersioningException.HttpException("Bad request")))
        Get(s"/files/configurations?project_id=${projectId.value}&project_file_path=$path&version=$versionString") ~> projectFileController
          .route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
          entityAs[String] shouldBe "Bad request"
        }
      }

      "return failed for invalid file" in {
        when(projectFileService.buildConfiguration(projectId, Paths.get(path), versionOption, accessToken.userId))
          .thenReturn(Future.failed(ValidationError(List("invalid some field"))))
        Get(s"/files/configurations?project_id=${projectId.value}&project_file_path=$path&version=$versionString") ~> projectFileController
          .route(accessToken) ~> check {
          status shouldBe StatusCodes.UnprocessableEntity
          entityAs[List[String]] shouldBe List("invalid some field")
        }
      }
    }
  }
}
