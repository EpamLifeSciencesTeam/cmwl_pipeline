package cromwell.pipeline.service
import cromwell.pipeline.datastorage.dto.{ ProjectId, Run, RunCreateRequest, RunUpdateRequest }
import cromwell.pipeline.model.wrapper.{ RunId, UserId }

import scala.concurrent.Future

class RunServiceTestImpl(runs: Seq[Run], testMode: TestMode) extends RunService {

  override def addRun(runCreateRequest: RunCreateRequest, projectId: ProjectId, userId: UserId): Future[RunId] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(RunId.random)
    }

  override def getRunByIdAndUser(runId: RunId, projectId: ProjectId, userId: UserId): Future[Option[Run]] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(runs.headOption)
    }

  override def getRunsByProject(projectId: ProjectId, userId: UserId): Future[Seq[Run]] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(runs)
    }

  override def deleteRunById(runId: RunId, projectId: ProjectId, userId: UserId): Future[Int] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(1)
    }

  override def updateRun(runId: RunId, request: RunUpdateRequest, projectId: ProjectId, userId: UserId): Future[Int] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(1)
    }

}

object RunServiceTestImpl {

  def apply(testRuns: Run*)(implicit testMode: TestMode = Success): RunServiceTestImpl =
    new RunServiceTestImpl(testRuns, testMode)

}
