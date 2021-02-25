package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.utils.{ TestProjectUtils, TestRunUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto.{
  CromwellInput,
  FileParameter,
  FileTree,
  PipelineVersion,
  Project,
  ProjectConfiguration,
  ProjectFile,
  ProjectFileConfiguration,
  ProjectFileContent,
  StringTyped
}
import cromwell.pipeline.service.Exceptions.ProjectNotFoundException
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import java.nio.file.Paths
import scala.concurrent.Future

class AggregationServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {

  private val projectService: ProjectService = mock[ProjectService]
  private val projectConfigurationService: ProjectConfigurationService = mock[ProjectConfigurationService]
  private val projectVersioning: GitLabProjectVersioning = mock[GitLabProjectVersioning]
  private val aggregatorService: AggregationService =
    new AggregationService(projectService, projectVersioning, projectConfigurationService)

  "AggregatorService" when {
    "aggregate" should {
      val dummyProject: Project = TestProjectUtils.getDummyProject()
      val projectId = dummyProject.projectId
      val runId = TestRunUtils.getDummyRunId
      val userId = TestUserUtils.getDummyUserId
      val path = Paths.get("test.wdl")
      val run = TestRunUtils.getDummyRun(runId = runId, projectId = projectId, userId = userId)
      val version = PipelineVersion(run.projectVersion)
      val projectConfigurations =
        ProjectConfiguration(
          projectId,
          active = true,
          List(
            ProjectFileConfiguration(path, List(FileParameter("_type", StringTyped(Some("String")))))
          )
        )

      "should return CromwellInput" taggedAs Service in {
        val trees = Seq(FileTree("1", "test", path.toString, "created"))
        val file = ProjectFile(path, ProjectFileContent(""))

        val result = CromwellInput(
          projectId,
          userId,
          version,
          List(file),
          List(
            ProjectFileConfiguration(Paths.get("test.wdl"), List(FileParameter("_type", StringTyped(Some("String")))))
          )
        )

        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(dummyProject))
        when(projectVersioning.getFilesTree(dummyProject, Some(version))).thenReturn(Future.successful(Right(trees)))
        when(projectVersioning.getFile(dummyProject, path, Some(version))).thenReturn(Future.successful(Right(file)))
        when(projectConfigurationService.getById(projectId)).thenReturn(Future.successful(Some(projectConfigurations)))

        aggregatorService.aggregate(run).map {
          _ shouldBe result
        }
      }

      "should return exception if project not found" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, userId))
          .thenReturn(Future.failed(new ProjectNotFoundException))
        when(projectConfigurationService.getById(projectId)).thenReturn(Future.successful(Some(projectConfigurations)))

        aggregatorService.aggregate(run).failed.map {
          _ should have.message("Project not found")
        }
      }

      "should return exception if configuration not found" taggedAs Service in {
        val trees = Seq(FileTree("1", "test", path.toString, "created"))
        val file = ProjectFile(path, ProjectFileContent(""))

        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(dummyProject))
        when(projectVersioning.getFilesTree(dummyProject, Some(version))).thenReturn(Future.successful(Right(trees)))
        when(projectVersioning.getFile(dummyProject, path, Some(version))).thenReturn(Future.successful(Right(file)))
        when(projectConfigurationService.getById(projectId)).thenReturn(Future.successful(None))

        aggregatorService.aggregate(run).failed.map {
          _ should have.message("Configurations for projectId " + projectId + " not found")
        }
      }

      "should return exception if file not found" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(dummyProject))
        when(projectVersioning.getFilesTree(dummyProject, Some(version)))
          .thenReturn(Future.successful(Left(VersioningException.FileException("Could not take the files tree"))))
        when(projectConfigurationService.getById(projectId)).thenReturn(Future.successful(Some(projectConfigurations)))

        aggregatorService.aggregate(run).failed.map {
          _ should have.message("Could not take the files tree")
        }
      }
    }
  }
}
