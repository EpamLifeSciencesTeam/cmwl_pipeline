package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dao.utils.{ TestProjectUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.service.VersioningException.FileException
import cromwell.pipeline.service.{ ProjectFileService, VersioningException }
import cromwell.pipeline.utils.URLEncoderUtils
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import java.nio.file.Paths
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class ProjectFileControllerTest extends AsyncWordSpec with Matchers with ScalatestRouteTest with MockitoSugar {
  private val projectFileService: ProjectFileService = mock[ProjectFileService]
  private val projectFileController = new ProjectFileController(projectFileService)

  "ProjectFileController" when {
    val accessToken = AccessTokenContent(TestUserUtils.getDummyUserId)
    val versionString = "v0.0.2"
    val version = PipelineVersion(versionString)
    val versionOption = Some(version)
    val projectId = TestProjectUtils.getDummyProjectId
    val path = Paths.get("folder/file.wdl")
    val pathString = URLEncoderUtils.encode(path.toString)
    val projectFileContent = ProjectFileContent("task hello {}")
    val projectFile = ProjectFile(path, projectFileContent)

    "validate file" should {
      val request = ValidateFileContentRequest(projectFileContent)

      "return OK response to valid file" taggedAs Controller in {
        when(projectFileService.validateFile(projectFileContent)).thenReturn(Future.successful(Right(())))
        Post(s"/files/validation", request) ~> projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
        }
      }

      "return error response to invalid file" taggedAs Controller in {
        when(projectFileService.validateFile(projectFileContent))
          .thenReturn(Future.successful(Left(ValidationError(List("Miss close bracket")))))
        Post(s"/files/validation", request) ~> projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.Conflict
          entityAs[List[String]] shouldBe List("Miss close bracket")
        }
      }
    }

    "upload file" should {
      val projectFile = ProjectFile(path, projectFileContent)
      val request = ProjectUpdateFileRequest(projectFile, Some(version))

      "return OK response for valid request with a valid file" taggedAs Controller in {
        when(projectFileService.validateFile(projectFileContent)).thenReturn(Future.successful(Right(())))
        when(projectFileService.uploadFile(projectId, projectFile, Some(version), accessToken.userId))
          .thenReturn(Future.successful(Right(())))
        Post(s"/projects/${projectId.value}/files", request) ~> projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
        }
      }

      "return Precondition File response for valid request with an invalid file" taggedAs Controller in {
        when(projectFileService.validateFile(projectFileContent))
          .thenReturn(Future.successful(Left(ValidationError(List("Miss close bracket")))))
        when(projectFileService.uploadFile(projectId, projectFile, Some(version), accessToken.userId))
          .thenReturn(Future.successful(Right(())))
        Post(s"/projects/${projectId.value}/files", request) ~> projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.Created
        }
      }

      "return UnprocessableEntity for bad request" taggedAs Controller in {
        when(projectFileService.uploadFile(projectId, projectFile, Some(version), accessToken.userId))
          .thenReturn(Future.successful(Left(VersioningException.HttpException("Bad request"))))
        Post(s"/projects/${projectId.value}/files", request) ~> projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.UnprocessableEntity
          entityAs[String] shouldBe "File have not uploaded due to Bad request"
        }
      }
    }

    "get files" should {
      "return files with status code OK" in {
        when(projectFileService.getFiles(projectId, versionOption, accessToken.userId))
          .thenReturn(Future.successful(List(projectFile)))
        Get(s"/projects/${projectId.value}/files?version=$versionString") ~>
        projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          entityAs[List[ProjectFile]] shouldBe List(projectFile)
        }
      }

      "return files with status code OK without version" in {
        when(projectFileService.getFiles(projectId, None, accessToken.userId))
          .thenReturn(Future.successful(List(projectFile)))
        Get(s"/projects/${projectId.value}/files") ~>
        projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          entityAs[List[ProjectFile]] shouldBe List(projectFile)
        }
      }

      "return 404 if files not found" in {
        val versioningException = FileException("Something went wrong")

        when(projectFileService.getFiles(projectId, None, accessToken.userId))
          .thenReturn(Future.failed(versioningException))
        Get(s"/projects/${projectId.value}/files") ~>
        projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NotFound
        }
      }
    }

    "get file" should {
      "return file with status code OK" in {
        when(projectFileService.getFile(projectId, path, versionOption, accessToken.userId))
          .thenReturn(Future.successful(projectFile))
        Get(s"/projects/${projectId.value}/files/$pathString?version=$versionString") ~>
        projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          entityAs[ProjectFile] shouldBe projectFile
        }
      }

      "return file with status code OK without version" in {
        when(projectFileService.getFile(projectId, path, None, accessToken.userId))
          .thenReturn(Future.successful(projectFile))
        Get(s"/projects/${projectId.value}/files/$pathString") ~> projectFileController.route(
          accessToken
        ) ~> check {
          status shouldBe StatusCodes.OK
          entityAs[ProjectFile] shouldBe projectFile
        }
      }

      "return 404 if file not found" in {
        val versioningException = FileException("Something went wrong")

        when(projectFileService.getFile(projectId, path, versionOption, accessToken.userId))
          .thenReturn(Future.failed(versioningException))
        Get(s"/projects/${projectId.value}/files/$pathString?version=$versionString") ~> projectFileController.route(
          accessToken
        ) ~> check {
          status shouldBe StatusCodes.NotFound
        }
      }
    }
  }
}
