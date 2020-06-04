package cromwell.pipeline.service

import java.nio.file.Paths

import cats.data.NonEmptyList
import cromwell.pipeline.datastorage.dao.repository.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto.{ Commit, FileContent, ProjectFile, ValidationError, Version }
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
      val versionName = "v.0.0.2"
      val version = Version("v.0.0.2", "new version", "this project", Commit("commit_12"))

      when(projectVersioning.getFilesVersions(project, projectFile.path))
        .thenReturn(Future.successful(Right(List(version))))

      "return success message for request" taggedAs Service in {
        when(projectVersioning.updateFile(project, projectFile, Some(version)))
          .thenReturn(Future.successful(Right("Success")))
        projectFileService.uploadFile(project, projectFile, Some(versionName)).map(_ shouldBe Right("Success"))
      }

      "return error message for error request" taggedAs Service in {
        when(projectVersioning.updateFile(project, projectFile, Some(version)))
          .thenReturn(Future.successful(Left(VersioningException("Something wrong"))))
        projectFileService
          .uploadFile(project, projectFile, Some(versionName))
          .map(_ shouldBe Left(VersioningException("Something wrong")))
      }
    }
  }
}
