package cromwell.pipeline.service
import cromwell.pipeline.datastorage.dto.{ Run, RunCreateRequest, RunUpdateRequest }
import cromwell.pipeline.model.wrapper.{ RunId, UserId }

import scala.concurrent.Future

class RunServiceTestImpl(runs: Seq[Run], testMode: TestMode) extends RunService {

  override def addRun(runCreateRequest: RunCreateRequest, userId: UserId): Future[RunId] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(RunId.random)
    }

  override def getRunByIdAndUser(runId: RunId, userId: UserId): Future[Option[Run]] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(runs.headOption)
    }

  override def deleteRunById(runId: RunId, userId: UserId): Future[Int] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(1)
    }

  override def updateRun(runId: RunId, request: RunUpdateRequest, userId: UserId): Future[Int] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(1)
    }

}

object RunServiceTestImpl {

  def apply(testRuns: Run*)(implicit testMode: TestMode = Success): RunServiceTestImpl =
    new RunServiceTestImpl(testRuns, testMode)

}
