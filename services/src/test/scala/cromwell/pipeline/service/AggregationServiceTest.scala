package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.utils.{ TestProjectUtils, TestRunUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.service.ProjectService.Exceptions.ProjectNotFoundException
import cromwell.pipeline.service.impls.{
  ProjectConfigurationServiceTestImpl,
  ProjectServiceTestImpl,
  ProjectVersioningTestImpl
}
import org.scalatest.{ AsyncWordSpec, Matchers }

import java.nio.file.Paths

class AggregationServiceTest extends AsyncWordSpec with Matchers {

  "AggregatorService" when {
    "aggregate" should {
      val dummyProject: Project = TestProjectUtils.getDummyProject()
      val projectId = dummyProject.projectId
      val runId = TestRunUtils.getDummyRunId
      val userId = TestUserUtils.getDummyUserId
      val path = Paths.get("test.wdl")
      val run = TestRunUtils.getDummyRun(runId = runId, projectId = projectId, userId = userId)
      val version = PipelineVersion(run.projectVersion)
      val configurationId = ProjectConfigurationId.randomId
      val projectConfigurations =
        ProjectConfiguration(
          configurationId,
          projectId,
          active = true,
          WdlParams(path, List(FileParameter("_type", StringTyped(Some("String"))))),
          ProjectConfigurationVersion.defaultVersion
        )

      "should return CromwellInput" taggedAs Service in {
        val file = ProjectFile(path, ProjectFileContent(""))

        val result = CromwellInput(
          projectId,
          userId,
          version,
          List(file),
          WdlParams(Paths.get("test.wdl"), List(FileParameter("_type", StringTyped(Some("String")))))
        )
        val projectService = ProjectServiceTestImpl(dummyProject)
        val projectVersioning = ProjectVersioningTestImpl(projectFiles = List(file))
        val projectConfigurationService = ProjectConfigurationServiceTestImpl(projectConfigurations)
        val aggregatorService = AggregationService(projectService, projectVersioning, projectConfigurationService)

        aggregatorService.aggregate(run).map {
          _ shouldBe result
        }
      }

      "should return exception if project not found" taggedAs Service in {

        val projectService = ProjectServiceTestImpl.withException(new ProjectNotFoundException)
        val projectVersioning = ProjectVersioningTestImpl()
        val projectConfigurationService = ProjectConfigurationServiceTestImpl(projectConfigurations)
        val aggregatorService = AggregationService(projectService, projectVersioning, projectConfigurationService)

        aggregatorService.aggregate(run).failed.map {
          _ should have.message("Project not found")
        }
      }

      "should return exception if configuration not found" taggedAs Service in {
        val file = ProjectFile(path, ProjectFileContent(""))

        val projectService = ProjectServiceTestImpl(dummyProject)
        val projectVersioning = ProjectVersioningTestImpl(projectFiles = List(file))
        val projectConfigurationService = ProjectConfigurationServiceTestImpl()
        val aggregatorService = AggregationService(projectService, projectVersioning, projectConfigurationService)

        aggregatorService.aggregate(run).failed.map {
          _ should have.message("Configurations for projectId " + projectId + " not found")
        }
      }

      "should return exception if file not found" taggedAs Service in {

        val projectService = ProjectServiceTestImpl(dummyProject)
        val projectVersioning =
          ProjectVersioningTestImpl.withException(VersioningException.FileException("Could not take the files tree"))
        val projectConfigurationService = ProjectConfigurationServiceTestImpl(projectConfigurations)
        val aggregatorService = AggregationService(projectService, projectVersioning, projectConfigurationService)

        aggregatorService.aggregate(run).failed.map {
          _ should have.message("Could not take the files tree")
        }
      }
    }
  }
}
