package cromwell.pipeline.controller

import java.net.URLEncoder
import java.nio.file.Paths

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dao.repository.utils.{TestProjectUtils, TestUserUtils}
import cromwell.pipeline.datastorage.dto.ProjectBuildConfigurationRequest.ProjectUpdateFileRequest
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.service.{ProjectFileService, ProjectService, VersioningException}
import cromwell.pipeline.utils.{ApplicationConfig, GitLabConfig}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import org.mockito.Mockito.when
import org.scalatest.{AsyncWordSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class ProjectFileControllerTest extends AsyncWordSpec with Matchers with ScalatestRouteTest with MockitoSugar {
  private val projectFileService: ProjectFileService = mock[ProjectFileService]
  private val projectService: ProjectService = mock[ProjectService]
  private val projectFileController = new ProjectFileController(projectFileService, projectService)
  private val gitLabConfig: GitLabConfig = ApplicationConfig.load().gitLabConfig

  "ProjectFileController" when {
    val accessToken = AccessTokenContent(TestUserUtils.getDummyUserId)

    "validate file" should {
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
      val version = PipelineVersion("v0.0.2")
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
          status shouldBe StatusCodes.UnprocessableEntity
          entityAs[String] shouldBe "Bad request"
        }
      }
    }

    "delete file" should {
      val project = TestProjectUtils.getDummyProject()
      val branchName: String = gitLabConfig.defaultBranch
      val projectFile = ProjectFile(Paths.get("folder/test.txt"), "file context")
      val path = URLEncoder.encode(projectFile.path.toString, "UTF-8")
      val commitMessage = s"$path file has been deleted from $branchName"
      val commitMessageUrl = URLEncoder.encode(commitMessage, "UTF-8")
      val accessToken = AccessTokenContent(TestUserUtils.getDummyUserId)

      "return OK response for valid request" taggedAs Controller in {
        when(projectService.getProjectById(project.projectId)).thenReturn(Future.successful(Some(project)))
        when(projectFileService.deleteFile(project, projectFile.path, branchName, commitMessage))
          .thenReturn(Future.successful(Right("Success")))
        val url =
          s"/files?projectId=${project.projectId.value}&path=$path&branchName=$branchName&commitMessage=$commitMessageUrl"
        Delete(url) ~> projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
        }
      }
    }

    "get file" should {
      val project = TestProjectUtils.getDummyProject()
      val projectFile = ProjectFile(Paths.get("folder/test.txt"), "file context")
      val path = URLEncoder.encode(projectFile.path.toString, "UTF-8")
      val version = PipelineVersion("v0.0.2")
      val accessToken = AccessTokenContent(TestUserUtils.getDummyUserId)

      "return OK response for valid request" taggedAs Controller in {
        when(projectService.getProjectById(project.projectId)).thenReturn(Future.successful(Some(project)))
        when(projectFileService.getFile(project, projectFile.path, Some(version)))
          .thenReturn(Future.successful(Right(projectFile)))
        val url = s"/files?projectId=${project.projectId.value}&path=$path&version=${version.toString}"
        Get(url) ~> projectFileController.route(
          accessToken
        ) ~> check {
          status shouldBe StatusCodes.OK
        }
      }
    }

    "build configuration" should {
      val projectId = TestProjectUtils.getDummyProjectId
      val projectFile = ProjectFile(Paths.get("/home/test/file"), "{some context}")
      val configuration = ProjectConfiguration(
        projectId,
        List(
          ProjectFileConfiguration(
            Paths.get("/home/test/file"),
            List(FileParameter("nodeName", StringTyped(Some("hello"))))
          )
        )
      )
      val request = ProjectBuildConfigurationRequest(projectId, projectFile)

      "return configuration for file" in {
        when(projectFileService.buildConfiguration(projectId, projectFile))
          .thenReturn(Future.successful(Right(configuration)))
        Post("/files/configurations", request) ~> projectFileController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          entityAs[ProjectConfiguration] shouldBe configuration
        }
      }
    }
  }
}
