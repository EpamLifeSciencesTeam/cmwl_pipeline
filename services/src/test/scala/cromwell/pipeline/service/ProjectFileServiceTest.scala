package cromwell.pipeline.service

import cats.data.NonEmptyList
import cromwell.pipeline.datastorage.dao.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.womtool.WomTool
import java.nio.file.Paths
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar
import scala.concurrent.Future
import wom.executable.WomBundle

class ProjectFileServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {

  private val womTool = mock[WomTool]
  private val projectVersioning = mock[ProjectVersioning[VersioningException]]
  private val projectService = mock[ProjectService]
  private val configurationService = mock[ProjectConfigurationService]
  private val projectFileService =
    ProjectFileService(projectService, configurationService, womTool, projectVersioning)

  private val correctWdl = "task hello {}"
  private val incorrectWdl = "task hello {"
  private val errorMessage = "ERROR: miss bracket"

  "ProjectFileServiceTest" when {
    val userId = UserId.random
    val project = TestProjectUtils.getDummyProject()
    val projectId = project.projectId
    val projectFilePath = Paths.get("test.txt")
    val projectFileContent = ProjectFileContent(correctWdl)
    val projectFile = ProjectFile(projectFilePath, projectFileContent)
    val version = TestProjectUtils.getDummyPipeLineVersion()
    val optionVersion = Some(version)

    "validateFile" should {
      "return valid message to valid file" taggedAs Service in {
        val request = ProjectFileContent(correctWdl)
        val dummyBundle = WomBundle(None, Map.empty, Map.empty, Set.empty)
        when(womTool.validate(correctWdl)).thenReturn(Right(dummyBundle))
        projectFileService.validateFile(request).map(_ shouldBe Right(()))
      }

      "return error message to incorrect file" taggedAs Service in {
        val request = ProjectFileContent(incorrectWdl)
        when(womTool.validate(incorrectWdl)).thenReturn(Left(NonEmptyList(errorMessage, Nil)))
        projectFileService.validateFile(request).map(_ shouldBe Left(ValidationError(List(errorMessage))))
      }
    }

    "upload file" should {
      "return success for request" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(project))
        when(projectVersioning.updateFile(project, projectFile, Some(version)))
          .thenReturn(Future.successful(Right(version)))
        when(projectService.updateProjectVersion(projectId, version, userId)).thenReturn(Future.successful(0))
        projectFileService.uploadFile(projectId, projectFile, optionVersion, userId).map(_ shouldBe Right(()))
      }

      "return error message for error request if it couldn't update file" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(project))
        when(projectVersioning.updateFile(project, projectFile, Some(version)))
          .thenReturn(Future.successful(Left(VersioningException.HttpException("Something wrong"))))
        projectFileService
          .uploadFile(projectId, projectFile, Some(version), userId)
          .map(_ shouldBe Left(VersioningException.HttpException("Something wrong")))
      }
    }

    "get file" should {
      "return file with full request" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(project))
        when(projectVersioning.getFile(project, projectFilePath, optionVersion))
          .thenReturn(Future.successful(Right(projectFile)))
        projectFileService.getFile(projectId, projectFilePath, optionVersion, userId).map(_ shouldBe projectFile)
      }

      "return file with no version" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(project))
        when(projectVersioning.getFile(project, projectFilePath, None))
          .thenReturn(Future.successful(Right(projectFile)))
        projectFileService.getFile(projectId, projectFilePath, None, userId).map(_ shouldBe projectFile)
      }

      "return VersioningException when file not found" taggedAs Service in {
        val versioningException = VersioningException.HttpException(s"Exception. Response status: Not Found")
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(project))
        when(projectVersioning.getFile(project, projectFilePath, None))
          .thenReturn(Future.successful(Left(versioningException)))
        projectFileService.getFile(projectId, projectFilePath, None, userId).failed.map(_ shouldBe versioningException)
      }
    }

    "get files" should {
      "return files with full request" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(project))
        when(projectVersioning.getFiles(project, optionVersion)).thenReturn(Future.successful(Right(List(projectFile))))
        projectFileService.getFiles(projectId, optionVersion, userId).map(_ shouldBe List(projectFile))
      }

      "return files with no version" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(project))
        when(projectVersioning.getFiles(project, None)).thenReturn(Future.successful(Right(List(projectFile))))
        projectFileService.getFiles(projectId, None, userId).map(_ shouldBe List(projectFile))
      }

      "return VersioningException when file not found" taggedAs Service in {
        val versioningException = VersioningException.HttpException(s"Exception. Response status: Not Found")
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(project))
        when(projectVersioning.getFiles(project, None)).thenReturn(Future.failed(versioningException))
        projectFileService.getFiles(projectId, None, userId).failed.map(_ shouldBe versioningException)
      }
    }
  }
}
