package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.impls.RunRepositoryTestImpl
import cromwell.pipeline.datastorage.dao.utils.{ TestProjectUtils, TestRunUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto.{ Done, RunCreateRequest, RunUpdateRequest }
import cromwell.pipeline.service.impls.ProjectServiceTestImpl
import org.scalatest.{ AsyncWordSpec, Matchers }

class RunServiceTest extends AsyncWordSpec with Matchers {

  "RunService" when {
    val userId = TestUserUtils.getDummyUserId
    val runId = TestRunUtils.getDummyRunId
    val project = TestProjectUtils.getDummyProject(ownerId = userId)
    val projectId = project.projectId
    val run = TestRunUtils.getDummyRun(runId = runId, projectId = projectId, userId = userId)

    val runUpdated = run.copy(
      status = Done,
      timeStart = run.timeStart,
      timeEnd = TestRunUtils.getDummyTimeEnd(false),
      results = "new-results",
      cmwlWorkflowId = TestRunUtils.getDummyCmwlWorkflowId(false)
    )

    val request = RunUpdateRequest(
      runUpdated.status,
      runUpdated.timeStart,
      runUpdated.timeEnd,
      runUpdated.results,
      runUpdated.cmwlWorkflowId
    )
    val runServiceAccessError = RunService.Exceptions.AccessDenied()
    val projectServiceAccessError = ProjectService.Exceptions.AccessDenied()
    val runServiceNotFoundError = RunService.Exceptions.NotFound()

    def defaultRunRepository: RunRepositoryTestImpl = RunRepositoryTestImpl(run)
    val defaultProjectService: ProjectServiceTestImpl = ProjectServiceTestImpl(project)

    def createRunService(
      runRepository: RunRepositoryTestImpl = defaultRunRepository,
      projectService: ProjectServiceTestImpl = defaultProjectService
    ): RunService =
      RunService(runRepository, projectService)

    def runService: RunService = createRunService()

    "addRun" should {
      "returns run id" taggedAs Service in {
        val runCreateRequest = RunCreateRequest(projectVersion = run.projectVersion, results = run.results)

        runService.addRun(runCreateRequest, projectId, userId).map { _ =>
          succeed
        }
      }
    }

    "getRunById when user is the owner" should {

      "return run if run exists" taggedAs Service in {

        runService.getRunByIdAndUser(runId, projectId, run.userId).map { _ shouldBe Some(run) }
      }

      "return none if the run is not found" taggedAs Service in {
        val emptyRunRepository = RunRepositoryTestImpl()

        val runService = createRunService(runRepository = emptyRunRepository)

        runService.getRunByIdAndUser(runId, projectId, run.userId).map { _ shouldBe None }
      }

      "return none if run belongs to another project of the same user" taggedAs Service in {
        val anotherProjectRun = TestRunUtils.getDummyRun(userId = run.userId)
        val runRepositoryWithAnotherProject = RunRepositoryTestImpl(anotherProjectRun)

        val runService = createRunService(runRepository = runRepositoryWithAnotherProject)

        runService.getRunByIdAndUser(anotherProjectRun.runId, projectId, run.userId).map {
          _ shouldBe None
        }
      }
    }

    "getRunById when user is NOT the owner" should {

      "fail with access denied exception" taggedAs Service in {
        val errorProjectService = ProjectServiceTestImpl.withException(projectServiceAccessError)

        val runService = createRunService(projectService = errorProjectService)

        runService.getRunByIdAndUser(runId, projectId, userId).failed.map { _ shouldBe runServiceAccessError }
      }
    }

    "getRunsByProject" should {
      val runSeq = Seq(run)

      "return runs Seq if user is the owner" taggedAs Service in {
        val runRepository = RunRepositoryTestImpl(run)
        val runService = createRunService(runRepository = runRepository)

        runService.getRunsByProject(projectId, run.userId).map { _ shouldBe runSeq }
      }

      "return empty Seq if there are no runs" taggedAs Service in {
        val emptyRunRepository = RunRepositoryTestImpl()

        val runService = createRunService(runRepository = emptyRunRepository)

        runService.getRunsByProject(projectId, run.userId).map { _ shouldBe Seq.empty }
      }

      "fail if user is NOT the owner" taggedAs Service in {
        val errorProjectService = ProjectServiceTestImpl.withException(projectServiceAccessError)

        val runService = createRunService(projectService = errorProjectService)

        runService.getRunsByProject(projectId, run.userId).failed.map { _ shouldBe runServiceAccessError }
      }
    }

    "deleteRunById" should {

      "return 1 if the entity was deleted" taggedAs Service in {

        runService.deleteRunById(runId, projectId, userId).map { _ shouldBe 1 }
      }

      "fail with exception if user is NOT the owner" taggedAs Service in {
        val errorProjectService = ProjectServiceTestImpl.withException(projectServiceAccessError)

        val runService = createRunService(projectService = errorProjectService)

        runService.deleteRunById(runId, projectId, userId).failed.map { _ shouldBe runServiceAccessError }
      }

      "fail with exception if run belongs to another project" taggedAs Service in {
        val anotherProjectRun = TestRunUtils.getDummyRun(userId = userId)
        val anotherProjectRunRepository = RunRepositoryTestImpl(anotherProjectRun)

        val runService = createRunService(runRepository = anotherProjectRunRepository)

        runService.deleteRunById(anotherProjectRun.runId, projectId, userId).failed.map {
          _ shouldBe runServiceNotFoundError
        }
      }
    }

    "updateRun" should {

      "return the entity updated when user is the owner" taggedAs Service in {

        runService.updateRun(runId, request, projectId, run.userId).map { _ shouldBe 1 }
      }

      "fail with access denied exception when user is NOT the owner" taggedAs Service in {
        val errorProjectService = ProjectServiceTestImpl.withException(projectServiceAccessError)

        val runService = createRunService(projectService = errorProjectService)

        runService.updateRun(runId, request, projectId, run.userId).failed.map { _ shouldBe runServiceAccessError }
      }

      "fail with exception if run belongs to another project" taggedAs Service in {
        val anotherProjectRun = TestRunUtils.getDummyRun(userId = userId)
        val anotherProjectRunRepository = RunRepositoryTestImpl(anotherProjectRun)

        val runService = createRunService(runRepository = anotherProjectRunRepository)

        runService.updateRun(anotherProjectRun.runId, request, projectId, run.userId).failed.map {
          _ shouldBe runServiceNotFoundError
        }
      }
    }
  }
}
