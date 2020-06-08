package cromwell.pipeline.controller

import java.nio.file.Paths

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dao.repository.utils.{ TestProjectUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.utils.auth.AccessTokenContent
import cromwell.pipeline.service.{ ProjectFileService, VersioningException }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar
import cromwell.pipeline.datastorage.formatters.ProjectFormatters._

import scala.concurrent.Future

class ProjectFileControllerTest extends AsyncWordSpec with Matchers with ScalatestRouteTest with MockitoSugar {
  private val projectFileService: ProjectFileService = mock[ProjectFileService]
  private val projectFileController = new ProjectFileController(projectFileService)

  "ProjectFileController" when {
    "validate file" should {
      val accessToken = AccessTokenContent(TestUserUtils.getDummyUserId)
      val content = FileContent("task hello {}")

      "return OK response to valid file" taggedAs Controller in {
        when(projectFileService.validateFile(content)).thenReturn(Future.successful(Right(())))
        Post("/files/validation", content) ~> projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
        }
      }

      "return error response to invalid file" taggedAs Controller in {
        when(projectFileService.validateFile(content))
          .thenReturn(Future.successful(Left(ValidationError(List("Miss close bracket")))))
        Post("/files/validation", content) ~> projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.Conflict
          entityAs[List[String]] shouldBe List("Miss close bracket")
        }
      }
    }

    "upload file" should {
      val version = Version("v.0.0.2", "commit message", "this project", Commit("commit_12"))
      val accessToken = AccessTokenContent(TestUserUtils.getDummyUserId)
      val project = TestProjectUtils.getDummyProject()
      val projectFile = ProjectFile(Paths.get("folder/test.txt"), "file context")
      val request = ProjectUpdateFileRequest(project, projectFile, Some(version))
      val content = FileContent(projectFile.content)

      "return OK response for valid request with a valid file" taggedAs Controller in {
        when(projectFileService.validateFile(content)).thenReturn(Future.successful(Right(())))
        when(projectFileService.uploadFile(project, projectFile, Some(version)))
          .thenReturn(Future.successful(Right("Success")))
        Post("/files", request) ~> projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
        }
      }

      "return Precondition File response for valid request with an invalid file" taggedAs Controller in {
        when(projectFileService.validateFile(content))
          .thenReturn(Future.successful(Left(ValidationError(List("Miss close bracket")))))
        when(projectFileService.uploadFile(project, projectFile, Some(version)))
          .thenReturn(Future.successful(Right("Success")))
        Post("/files", request) ~> projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.Created
        }
      }

      "return InternalServerError for bad request" taggedAs Controller in {
        when(projectFileService.uploadFile(project, projectFile, Some(version)))
          .thenReturn(Future.successful(Left(VersioningException("Bad request"))))
        Post("/files", request) ~> projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.ImATeapot // TODO: change status code
          entityAs[String] shouldBe "Bad request"
        }
      }
    }
  }
}
