package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.RunRepository
import cromwell.pipeline.datastorage.dao.utils.{ TestRunUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto.{ Done, Run, RunCreateRequest, RunUpdateRequest }
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class RunServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {

  private val runRepository: RunRepository = mock[RunRepository]
  private val runService: RunService = new RunService(runRepository)

  "RunService" when {
    "addRun" should {

      "returns run id" taggedAs Service in {
        val runId = TestRunUtils.getDummyRunId
        val projectId = TestRunUtils.getDummyProjectId
        val userId = TestUserUtils.getDummyUserId
        val run = TestRunUtils.getDummyRun(runId = runId, projectId = projectId, userId = userId)
        val runCreateRequest = RunCreateRequest(
          projectId = projectId,
          projectVersion = run.projectVersion,
          results = run.results,
          userId = userId
        )

        when(runRepository.addRun(any[Run])).thenReturn(Future.successful(runId))
        runService.addRun(runCreateRequest).map {
          _ shouldBe runId
        }
      }
    }

    "getRunById" should {

      "returns run entity if the entity exists" taggedAs Service in {

        val runId = TestRunUtils.getDummyRunId
        val run = TestRunUtils.getDummyRun(runId)
        when(runRepository.getRunByIdAndUser(runId, run.userId)).thenReturn(Future.successful(Some(run)))

        runService.getRunByIdAndUser(runId, run.userId).map { _ shouldBe Some(run) }
      }

      "returns none if the run is not found" taggedAs Service in {
        val runId = TestRunUtils.getDummyRunId
        val userId = TestUserUtils.getDummyUserId
        when(runRepository.getRunByIdAndUser(runId, userId)).thenReturn(Future(None))
        runService.getRunByIdAndUser(runId, userId).map { _ shouldBe None }
      }
    }

    "deleteRunById" should {

      "returns 1 if the entity was deleted" taggedAs Service in {
        val runId = TestRunUtils.getDummyRunId
        val run = TestRunUtils.getDummyRun(runId)
        when(runRepository.getRunByIdAndUser(runId, run.userId)).thenReturn(Future.successful(Some(run)))
        when(runRepository.deleteRunById(runId)).thenReturn(Future.successful(1))

        runService.deleteRunById(runId, run.userId).map { _ shouldBe 1 }
      }
    }

    "updateRun" should {

      "returns the entity updated" taggedAs Service in {
        val runId = TestRunUtils.getDummyRunId
        val run = TestRunUtils.getDummyRun(runId)
        val runUpdated = run.copy(
          status = Done,
          timeStart = run.timeStart,
          timeEnd = TestRunUtils.getDummyTimeEnd(false),
          results = "new-results",
          cmwlWorkflowId = TestRunUtils.getDummyCmwlWorkflowId(false)
        )

        val request = RunUpdateRequest(
          runId,
          runUpdated.status,
          runUpdated.timeStart,
          runUpdated.timeEnd,
          runUpdated.results,
          runUpdated.cmwlWorkflowId
        )

        when(runRepository.getRunByIdAndUser(runId, run.userId)).thenReturn(Future.successful(Some(run)))
        when(runRepository.updateRun(runUpdated)).thenReturn(Future.successful(1))
        runService.updateRun(request, run.userId).map { _ shouldBe 1 }
      }
    }
  }
}
