package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.service.impls.{
  ProjectConfigurationServiceTestImpl,
  ProjectServiceTestImpl,
  ProjectVersioningTestImpl,
  WomToolTestImpl
}
import cromwell.pipeline.womtool.WomToolAPI
import org.scalatest.{ AsyncWordSpec, Matchers }
import wom.executable.WomBundle

import java.nio.file.Paths

class ProjectFileServiceTest extends AsyncWordSpec with Matchers {

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
    val dummyBundles = List(WomBundle(None, Map.empty, Map.empty, Set.empty))

    val successProjectVersioning =
      ProjectVersioningTestImpl(projectFiles = List(projectFile), pipelineVersions = List(version))
    val successProjectService = ProjectServiceTestImpl(project)
    val successWomTool = WomToolTestImpl(womBundles = dummyBundles)
    val successConfigurationService = ProjectConfigurationServiceTestImpl()

    def createProjectFileService(
      projectService: ProjectService = successProjectService,
      configurationService: ProjectConfigurationService = successConfigurationService,
      womTool: WomToolAPI = successWomTool,
      projectVersioning: ProjectVersioning[VersioningException] = successProjectVersioning
    ): ProjectFileService =
      ProjectFileService(projectService, configurationService, womTool, projectVersioning)

    val projectFileService = createProjectFileService()

    "validateFile" should {
      "return valid message to valid file" taggedAs Service in {
        val request = ProjectFileContent(correctWdl)

        projectFileService.validateFile(request).map(_ shouldBe Right(()))
      }

      "return error message to incorrect file" taggedAs Service in {
        val request = ProjectFileContent(incorrectWdl)

        val projectFileService =
          createProjectFileService(womTool = WomToolTestImpl.withErrorMessages(List(errorMessage)))

        projectFileService.validateFile(request).map(_ shouldBe Left(ValidationError(List(errorMessage))))
      }
    }

    "upload file" should {
      "return success for request" taggedAs Service in {

        projectFileService.uploadFile(projectId, projectFile, optionVersion, userId).map(_ shouldBe Right(()))
      }

      "return error message for error request if it couldn't update file" taggedAs Service in {
        val versioningException = VersioningException.HttpException("Something wrong")

        val projectFileService = createProjectFileService(
          projectVersioning = ProjectVersioningTestImpl.withException(versioningException)
        )

        projectFileService
          .uploadFile(projectId, projectFile, Some(version), userId)
          .map(_ shouldBe Left(VersioningException.HttpException("Something wrong")))
      }
    }

    "delete file" should {
      "return success for request" taggedAs Service in {
        projectFileService.deleteFile(projectId, projectFilePath, optionVersion, userId).map(_ shouldBe Right(()))
      }

      "return error message if was unable to delete file" taggedAs Service in {
        val exceptionMsg = s"Failed to delete file: $projectFilePath from project: ${project.name}"

        val repositoryException = VersioningException.RepositoryException(exceptionMsg)

        val projectFileService = createProjectFileService(
          projectVersioning = ProjectVersioningTestImpl.withException(repositoryException)
        )

        projectFileService
          .deleteFile(projectId, projectFilePath, optionVersion, userId)
          .failed
          .map(_ shouldBe repositoryException)
      }
    }

    "get file" should {
      "return file with full request" taggedAs Service in {
        projectFileService.getFile(projectId, projectFilePath, optionVersion, userId).map(_ shouldBe projectFile)
      }

      "return file with no version" taggedAs Service in {
        val projectFileService =
          createProjectFileService(
            projectVersioning = ProjectVersioningTestImpl(projectFiles = List(projectFile), pipelineVersions = List())
          )

        projectFileService.getFile(projectId, projectFilePath, None, userId).map(_ shouldBe projectFile)
      }

      "return VersioningException when file not found" taggedAs Service in {
        val versioningException = VersioningException.HttpException(s"Exception. Response status: Not Found")
        val projectFileService =
          createProjectFileService(projectVersioning = ProjectVersioningTestImpl.withException(versioningException))

        projectFileService.getFile(projectId, projectFilePath, None, userId).failed.map(_ shouldBe versioningException)
      }
    }

    "get files" should {
      "return files with full request" taggedAs Service in {
        projectFileService.getFiles(projectId, optionVersion, userId).map(_ shouldBe List(projectFile))
      }

      "return files with no version" taggedAs Service in {
        projectFileService.getFiles(projectId, None, userId).map(_ shouldBe List(projectFile))
      }

      "return VersioningException when file not found" taggedAs Service in {
        val versioningException = VersioningException.HttpException(s"Exception. Response status: Not Found")
        val projectFileService =
          createProjectFileService(projectVersioning = ProjectVersioningTestImpl.withException(versioningException))

        projectFileService.getFiles(projectId, None, userId).failed.map(_ shouldBe versioningException)
      }
    }
  }
}
