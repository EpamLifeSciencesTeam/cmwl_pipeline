package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.utils.{ TestProjectUtils, TestRunUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.service.ProjectService.Exceptions.NotFound
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
      val file = ProjectFile(path, ProjectFileContent(""))

      val defaultProjectService = ProjectServiceTestImpl(dummyProject)
      val defaultProjectVersioning = ProjectVersioningTestImpl(projectFiles = List(file))
      val defaultProjectConfigurationService = ProjectConfigurationServiceTestImpl(projectConfigurations)

      def createAggregationService(
        projectService: ProjectService = defaultProjectService,
        projectVersioning: ProjectVersioning[VersioningException] = defaultProjectVersioning,
        projectConfigurationService: ProjectConfigurationService = defaultProjectConfigurationService
      ): AggregationService =
        AggregationService(projectService, projectVersioning, projectConfigurationService)

      val aggregatorService = createAggregationService()

      "should return CromwellInput" taggedAs Service in {

        val result = CromwellInput(
          projectId,
          userId,
          version,
          List(file),
          WdlParams(Paths.get("test.wdl"), List(FileParameter("_type", StringTyped(Some("String")))))
        )

        aggregatorService.aggregate(run).map {
          _ shouldBe result
        }
      }

      "should return exception if project not found" taggedAs Service in {

        val projectServiceWithExc = ProjectServiceTestImpl.withException(NotFound())
        val aggregatorService = createAggregationService(projectService = projectServiceWithExc)

        aggregatorService.aggregate(run).failed.map {
          _ should have.message("Project not found")
        }
      }

      "should return exception if configuration not found" taggedAs Service in {

        val emptyProjectConfigurationService = ProjectConfigurationServiceTestImpl.withException(
          NotFound(s"Configurations for projectId ${projectId.value} not found")
        )
        val aggregatorService = createAggregationService(projectConfigurationService = emptyProjectConfigurationService)

        aggregatorService.aggregate(run).failed.map {
          _ should have.message(s"Configurations for projectId ${projectId.value} not found")
        }
      }

      "should return exception if file not found" taggedAs Service in {

        val projectVersioningWithExc =
          ProjectVersioningTestImpl.withException(VersioningException.FileException("Could not take the files tree"))

        val aggregatorService = createAggregationService(projectVersioning = projectVersioningWithExc)

        aggregatorService.aggregate(run).failed.map {
          _ should have.message("Could not take the files tree")
        }
      }
    }
  }
}
