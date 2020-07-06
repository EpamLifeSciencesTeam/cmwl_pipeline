package cromwell.pipeline.service

import java.nio.file.{ Path, Paths }

import cats.data.NonEmptyList
import cromwell.pipeline.datastorage.dao.repository.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto.{ FileContent, PipelineVersion, ProjectFile, ValidationError }
import cromwell.pipeline.utils.{ ApplicationConfig, GitLabConfig }
import cromwell.pipeline.womtool.WomTool
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar
import wom.executable.WomBundle

import scala.concurrent.Future

class ProjectFileServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {

  private val womTool = mock[WomTool]
  private val projectVersioning = mock[ProjectVersioning[VersioningException]]
  private val projectFileService = new ProjectFileService(womTool, projectVersioning)
  private val gitLabConfig: GitLabConfig = ApplicationConfig.load().gitLabConfig

  private val correctWdl = "task hello {}"
  private val incorrectWdl = "task hello {"
  private val errorMessage = "ERROR: miss bracket"

  "ProjectFileServiceTest" when {
    "validateFile" should {
      "return valid message to valid file" taggedAs Service in {
        val request = FileContent(correctWdl)
        when(womTool.validate(correctWdl)).thenReturn(Right(any[WomBundle]))
        projectFileService.validateFile(request).map(_ shouldBe Right(()))
      }

      "return error message to incorrect file" taggedAs Service in {
        val request = FileContent(incorrectWdl)
        when(womTool.validate(incorrectWdl)).thenReturn(Left(NonEmptyList(errorMessage, Nil)))
        projectFileService.validateFile(request).map(_ shouldBe Left(ValidationError(List(errorMessage))))
      }
    }

    "upload file" should {
      val project = TestProjectUtils.getDummyProject()
      val projectFile = ProjectFile(Paths.get("test.txt"), "File content")
      val version = TestProjectUtils.getDummyPipeLineVersion()

      "return success message for request" taggedAs Service in {
        when(projectVersioning.updateFile(project, projectFile, Some(version)))
          .thenReturn(Future.successful(Right("Success")))
        projectFileService.uploadFile(project, projectFile, Some(version)).map(_ shouldBe Right("Success"))
      }

      "return error message for error request" taggedAs Service in {
        when(projectVersioning.updateFile(project, projectFile, Some(version)))
          .thenReturn(Future.successful(Left(VersioningException("Something wrong"))))
        projectFileService
          .uploadFile(project, projectFile, Some(version))
          .map(_ shouldBe Left(VersioningException("Something wrong")))
      }
    }

    "delete file" should {
      val project = TestProjectUtils.getDummyProject()
      val path: Path = Paths.get("test.md")
      val branchName: String = gitLabConfig.defaultBranch
      val commitMessage: String = s"$path file has been deleted from $branchName"

      "return success message for request" taggedAs Service in {
        when(projectVersioning.deleteFile(project, path, branchName, commitMessage))
          .thenReturn(Future.successful(Right("Success")))
        projectFileService.deleteFile(project, path, branchName, commitMessage).map(_ shouldBe Right("Success"))
      }

      "return error message for error request" taggedAs Service in {
        when(projectVersioning.deleteFile(project, path, branchName, commitMessage))
          .thenReturn(Future.successful(Left(VersioningException("Something wrong"))))
        projectFileService
          .deleteFile(project, path, branchName, commitMessage)
          .map(_ shouldBe Left(VersioningException("Something wrong")))
      }
    }

    "get file" should {
      val project = TestProjectUtils.getDummyProject()
      val file = Paths.get("folder/test.txt")
      val version = Some(PipelineVersion("v0.0.2"))

      "return success message for request" taggedAs Service in {
        when(projectVersioning.getFile(project, file, version))
          .thenReturn(Future.successful(Right(ProjectFile(file, "Success"))))
        projectFileService.getFile(project, file, version).map(_ shouldBe Right(ProjectFile(file, "Success")))
      }

      "return error message for error request" taggedAs Service in {
        when(projectVersioning.getFile(project, file, version))
          .thenReturn(Future.successful(Left(VersioningException("Something wrong"))))
        projectFileService.getFile(project, file, version).map(_ shouldBe Left(VersioningException("Something wrong")))
      }
    }
  }
}
