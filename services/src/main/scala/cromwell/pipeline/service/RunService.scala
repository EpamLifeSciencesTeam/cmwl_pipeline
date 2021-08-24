package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.RunRepository
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.{ RunId, UserId }

import java.time.Instant
import scala.concurrent.{ ExecutionContext, Future }

trait RunService {

  def addRun(runCreateRequest: RunCreateRequest, projectId: ProjectId, userId: UserId): Future[RunId]

  def getRunByIdAndUser(runId: RunId, projectId: ProjectId, userId: UserId): Future[Option[Run]]

  def getRunsByProject(projectId: ProjectId, userId: UserId): Future[Seq[Run]]

  def deleteRunById(runId: RunId, projectId: ProjectId, userId: UserId): Future[Int]

  def updateRun(runId: RunId, request: RunUpdateRequest, projectId: ProjectId, userId: UserId): Future[Int]

}

object RunService {

  def apply(runRepository: RunRepository, projectService: ProjectService)(
    implicit executionContext: ExecutionContext
  ): RunService =
    new RunService {

      def addRun(runCreateRequest: RunCreateRequest, projectId: ProjectId, userId: UserId): Future[RunId] = {
        val newRun = Run(
          runId = RunId.random,
          projectId = projectId,
          projectVersion = runCreateRequest.projectVersion,
          status = Created,
          timeStart = Instant.now(),
          userId = userId,
          results = runCreateRequest.results
        )
        projectService.getUserProjectById(projectId, userId).flatMap(_ => runRepository.addRun(newRun))
      }

      def getRunByIdAndUser(runId: RunId, projectId: ProjectId, userId: UserId): Future[Option[Run]] =
        projectService.getUserProjectById(projectId, userId).flatMap { _ =>
          runRepository.getRunByIdAndUser(runId, userId).map(_.filter(_.projectId == projectId))
        }

      def getRunsByProject(projectId: ProjectId, userId: UserId): Future[Seq[Run]] =
        projectService.getUserProjectById(projectId, userId).flatMap(_ => runRepository.getRunsByProject(projectId))

      def deleteRunById(runId: RunId, projectId: ProjectId, userId: UserId): Future[Int] =
        getRunByIdAndUser(runId, projectId, userId).flatMap {
          case Some(_) => runRepository.deleteRunById(runId)
          case None    => Future.failed(new RuntimeException("run with this id doesn't exist"))
        }

      def updateRun(runId: RunId, request: RunUpdateRequest, projectId: ProjectId, userId: UserId): Future[Int] =
        getRunByIdAndUser(runId, projectId, userId).flatMap {
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
