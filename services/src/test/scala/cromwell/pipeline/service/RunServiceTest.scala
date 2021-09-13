package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.RunRepository
import cromwell.pipeline.datastorage.dao.utils.{ TestProjectUtils, TestRunUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto.{ Done, Run, RunCreateRequest, RunUpdateRequest }
import cromwell.pipeline.service.ProjectService.Exceptions.AccessDenied
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class RunServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {

  private val runRepository: RunRepository = mock[RunRepository]
  private val projectService: ProjectService = mock[ProjectService]
  private val runService: RunService = RunService(runRepository, projectService)

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

    "addRun" should {

      "returns run id" taggedAs Service in {
        val runCreateRequest = RunCreateRequest(projectVersion = run.projectVersion, results = run.results)
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(project))
        when(runRepository.addRun(any[Run])).thenReturn(Future.successful(runId))

        runService.addRun(runCreateRequest, projectId, userId).map {
          _ shouldBe runId
        }
      }
    }

    "getRunById when user is the owner" should {

      "return run if run exists" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, run.userId)).thenReturn(Future.successful(project))
        when(runRepository.getRunByIdAndUser(runId, run.userId)).thenReturn(Future.successful(Some(run)))

        runService.getRunByIdAndUser(runId, projectId, run.userId).map { _ shouldBe Some(run) }
      }

      "return none if the run is not found" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, run.userId)).thenReturn(Future.successful(project))
        when(runRepository.getRunByIdAndUser(runId, run.userId)).thenReturn(Future(None))

        runService.getRunByIdAndUser(runId, projectId, run.userId).map { _ shouldBe None }
      }

      "return none if run belongs to another project of the same user" taggedAs Service in {
        val anotherProjectRun = TestRunUtils.getDummyRun(userId = run.userId)
        when(projectService.getUserProjectById(projectId, run.userId)).thenReturn(Future.successful(project))
        when(runRepository.getRunByIdAndUser(anotherProjectRun.runId, run.userId))
          .thenReturn(Future.successful(Some(anotherProjectRun)))

        runService.getRunByIdAndUser(anotherProjectRun.runId, projectId, run.userId).map {
          _ shouldBe None
        }
      }
    }

    "getRunById when user is NOT the owner" should {

      "fail with access denied exception" taggedAs Service in {
        val error = AccessDenied()
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.failed(error))

        runService.getRunByIdAndUser(runId, projectId, userId).failed.map { _ shouldBe error }
      }
    }

    "getRunsByProject" should {
      val runSeq = Seq(run)

      "return runs Seq if user is the owner" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, run.userId)).thenReturn(Future.successful(project))
        when(runRepository.getRunsByProject(projectId)).thenReturn(Future.successful(runSeq))

        runService.getRunsByProject(projectId, run.userId).map { _ shouldBe runSeq }
      }

      "return empty Seq if there are no runs" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, run.userId)).thenReturn(Future.successful(project))
        when(runRepository.getRunsByProject(projectId)).thenReturn(Future.successful(Seq.empty))

        runService.getRunsByProject(projectId, run.userId).map { _ shouldBe Seq.empty }
      }

      "fail if user is NOT the owner" taggedAs Service in {
        val error = AccessDenied()
        when(projectService.getUserProjectById(projectId, run.userId)).thenReturn(Future.failed(error))

        runService.getRunsByProject(projectId, run.userId).failed.map { _ shouldBe error }
      }
    }

    "deleteRunById" should {

      "return 1 if the entity was deleted" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(project))
        when(runRepository.getRunByIdAndUser(runId, userId)).thenReturn(Future.successful(Some(run)))
        when(runRepository.deleteRunById(runId)).thenReturn(Future.successful(1))

        runService.deleteRunById(runId, projectId, userId).map { _ shouldBe 1 }
      }

      "fail with exception if user is NOT the owner" taggedAs Service in {
        val error = AccessDenied()
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.failed(error))

        runService.deleteRunById(runId, projectId, userId).failed.map { _ shouldBe error }
      }

      "fail with exception if run belongs to another project" taggedAs Service in {
        val anotherProjectRun = TestRunUtils.getDummyRun(userId = userId)
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(project))
        when(runRepository.getRunByIdAndUser(anotherProjectRun.runId, userId))
          .thenReturn(Future.successful(Some(anotherProjectRun)))

        runService.deleteRunById(anotherProjectRun.runId, projectId, userId).failed.map {
          _.getMessage shouldBe "run with this id doesn't exist"
        }
      }
    }

    "updateRun" should {

      "return the entity updated when user is the owner" taggedAs Service in {
        when(projectService.getUserProjectById(projectId, run.userId)).thenReturn(Future.successful(project))
        when(runRepository.getRunByIdAndUser(runId, run.userId)).thenReturn(Future.successful(Some(run)))
        when(runRepository.updateRun(runUpdated)).thenReturn(Future.successful(1))

        runService.updateRun(runId, request, projectId, run.userId).map { _ shouldBe 1 }
      }

      "fail with access denied exception when user is NOT the owner" taggedAs Service in {
        val error = AccessDenied()
        when(projectService.getUserProjectById(projectId, run.userId)).thenReturn(Future.failed(error))

        runService.updateRun(runId, request, projectId, run.userId).failed.map { _ shouldBe error }
      }

      "fail with exception if run belongs to another project" taggedAs Service in {
        val anotherProjectRun = TestRunUtils.getDummyRun(userId = userId)
        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(project))
        when(runRepository.getRunByIdAndUser(anotherProjectRun.runId, userId))
          .thenReturn(Future.successful(Some(anotherProjectRun)))

        runService.updateRun(anotherProjectRun.runId, request, projectId, run.userId).failed.map {
          _.getMessage shouldBe "run with this id doesn't exist"
        }
      }
    }
  }
}
