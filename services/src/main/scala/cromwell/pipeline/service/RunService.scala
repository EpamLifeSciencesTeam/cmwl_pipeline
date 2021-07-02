package cromwell.pipeline.service

import java.time.Instant

import cromwell.pipeline.datastorage.dao.repository.RunRepository
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.{ RunId, UserId }

import scala.concurrent.{ ExecutionContext, Future }

trait RunService {

  def addRun(runCreateRequest: RunCreateRequest): Future[RunId]

  def getRunByIdAndUser(runId: RunId, userId: UserId): Future[Option[Run]]

  def deleteRunById(runId: RunId, userId: UserId): Future[Int]

  def updateRun(request: RunUpdateRequest, userId: UserId): Future[Int]

}

object RunService {

  def apply(runRepository: RunRepository)(implicit executionContext: ExecutionContext): RunService =
    new RunService {

      def addRun(runCreateRequest: RunCreateRequest): Future[RunId] = {

        val newRun = Run(
          runId = RunId.random,
          projectId = runCreateRequest.projectId,
          projectVersion = runCreateRequest.projectVersion,
          status = Created,
          timeStart = Instant.now(),
          userId = runCreateRequest.userId,
          results = runCreateRequest.results
        )

        runRepository.addRun(newRun)
      }

      def getRunByIdAndUser(runId: RunId, userId: UserId): Future[Option[Run]] =
        runRepository.getRunByIdAndUser(runId, userId)

      def deleteRunById(runId: RunId, userId: UserId): Future[Int] =
        runRepository.getRunByIdAndUser(runId, userId).flatMap {
          case Some(_) =>
            runRepository.deleteRunById(runId)
          case None => Future.failed(new RuntimeException("run with this id doesn't exist"))
        }
      def updateRun(request: RunUpdateRequest, userId: UserId): Future[Int] =
        runRepository.getRunByIdAndUser(request.runId, userId).flatMap {
          case Some(run) =>
            runRepository.updateRun(
              run.copy(
                status = request.status,
                timeStart = request.timeStart,
                timeEnd = request.timeEnd,
                results = request.results,
                cmwlWorkflowId = request.cmwlWorkflowId
              )
            )
          case None => Future.failed(new RuntimeException("run with this id doesn't exist"))
        }

    }

}
