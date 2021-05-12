package cromwell.pipeline.service

import cats.data.NonEmptyList
import cromwell.pipeline.datastorage.dao.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.womtool.WomTool
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar
import wom.executable.WomBundle

import java.nio.file.Paths
import scala.concurrent.Future

class ProjectFileServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {

  private val womTool = mock[WomTool]
  private val projectVersioning = mock[ProjectVersioning[VersioningException]]
  private val projectService = mock[ProjectService]
  private val projectFileService = new ProjectFileService(projectService, womTool, projectVersioning)

  private val correctWdl = "task hello {}"
  private val incorrectWdl = "task hello {"
  private val errorMessage = "ERROR: miss bracket"

  "ProjectFileServiceTest" when {
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
      val userId = UserId.random
      val project = TestProjectUtils.getDummyProject()
      val projectId = project.projectId
      val projectFileContent = ProjectFileContent("File content")
      val projectFile = ProjectFile(Paths.get("test.txt"), projectFileContent)
      val version = TestProjectUtils.getDummyPipeLineVersion()
      val updateFiledResponse = UpdateFiledResponse(projectFile.path.toString, "master")

      "return success message for request" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(project))
        when(projectVersioning.getUpdatedProjectVersion(project, Some(version)))
          .thenReturn(Future.successful(Right(version)))
        when(projectVersioning.updateFile(project, projectFile, version))
          .thenReturn(Future.successful(Right(updateFiledResponse)))
        when(projectService.updateProjectVersion(projectId, version, userId)).thenReturn(Future.successful(0))
        projectFileService
          .uploadFile(projectId, projectFile, Some(version), userId)
          .map(_ shouldBe Right(updateFiledResponse))
      }

      "return error message for error request if it couldn't get updated version" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(project))
        when(projectVersioning.getUpdatedProjectVersion(project, Some(version)))
          .thenReturn(Future.successful(Left(VersioningException.HttpException("Something wrong"))))
        projectFileService
          .uploadFile(projectId, projectFile, Some(version), userId)
          .map(_ shouldBe Left(VersioningException.HttpException("Something wrong")))
      }

      "return error message for error request if it couldn't update file" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(project))
        when(projectVersioning.getUpdatedProjectVersion(project, Some(version)))
          .thenReturn(Future.successful(Right(version)))
        when(projectVersioning.updateFile(project, projectFile, version))
          .thenReturn(Future.successful(Left(VersioningException.HttpException("Something wrong"))))
        projectFileService
          .uploadFile(projectId, projectFile, Some(version), userId)
          .map(_ shouldBe Left(VersioningException.HttpException("Something wrong")))
      }
    }

    "build configuration" should {
      val project = TestProjectUtils.getDummyProject()
      val projectId = project.projectId
      val projectFileContent = ProjectFileContent("File content")
      val projectFile = ProjectFile(Paths.get("test.txt"), projectFileContent)
      val version = Some(TestProjectUtils.getDummyPipeLineVersion())
      val userId = UserId.random

      "return success message for request" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(project))
        when(projectVersioning.getFile(project, projectFile.path, version))
          .thenReturn(Future.successful(Right(projectFile)))
        when(womTool.inputsToList(projectFileContent.content)).thenReturn(Right(Nil))
        projectFileService
          .buildConfiguration(projectId, projectFile.path, version, userId)
          .map(
            _ shouldBe ProjectConfiguration(
              projectId,
              active = true,
              List(ProjectFileConfiguration(projectFile.path, Nil))
            )
          )
      }

      "return error message for invalid file request" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(project))
        when(projectVersioning.getFile(project, projectFile.path, version))
          .thenReturn(Future.successful(Right(projectFile)))
        when(womTool.inputsToList(projectFileContent.content)).thenReturn(Left(NonEmptyList(errorMessage, Nil)))
        projectFileService
          .buildConfiguration(projectId, projectFile.path, version, userId)
          .failed
          .map(_ shouldBe ValidationError(List(errorMessage)))
      }

      "return error message for error request" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(project))
        when(projectVersioning.getFile(project, projectFile.path, version))
          .thenReturn(Future.successful(Left(VersioningException.FileException("404"))))
        projectFileService
          .buildConfiguration(projectId, projectFile.path, version, userId)
          .failed
          .map(_ shouldBe VersioningException.FileException("404"))
      }
    }
  }
}
