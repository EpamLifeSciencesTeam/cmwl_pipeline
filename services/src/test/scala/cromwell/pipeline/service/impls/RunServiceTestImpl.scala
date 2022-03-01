package cromwell.pipeline.service.impls

import cromwell.pipeline.datastorage.dto.{ Run, RunCreateRequest, RunUpdateRequest }
import cromwell.pipeline.model.wrapper.{ ProjectId, RunId, UserId }
import cromwell.pipeline.service.RunService

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

  def apply(runs: Run*): RunServiceTestImpl =
    new RunServiceTestImpl(runs = runs, testMode = Success)

  def withException(exception: Throwable): RunServiceTestImpl =
    new RunServiceTestImpl(runs = Seq.empty, testMode = WithException(exception))

}
