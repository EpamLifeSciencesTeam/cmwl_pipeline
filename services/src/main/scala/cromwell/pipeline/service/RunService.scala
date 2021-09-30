package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.RunRepository
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.{ RunId, UserId }
import cromwell.pipeline.service.ProjectService.Exceptions.ProjectServiceException
import cromwell.pipeline.service.RunService.Exceptions._
import cromwell.pipeline.service.exceptions.ServiceException

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
  object Exceptions {
    sealed abstract class RunServiceException(message: String) extends ServiceException(message)

    final case class AccessDenied(message: String = "Access denied") extends RunServiceException(message)
    final case class NotFound(message: String = "Run with this id doesn't exist") extends RunServiceException(message)
    final case class InternalError(message: String = "Internal error") extends RunServiceException(message)
  }

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
        projectService
          .getUserProjectById(projectId, userId)
          .recoverWith {
            case e: ProjectServiceException => serviceErrorMapper(e)
          }
          .flatMap(_ => runRepository.addRun(newRun))
      }

      def getRunByIdAndUser(runId: RunId, projectId: ProjectId, userId: UserId): Future[Option[Run]] =
        projectService
          .getUserProjectById(projectId, userId)
          .recoverWith {
            case e: ProjectServiceException => serviceErrorMapper(e)
          }
          .flatMap { _ =>
            runRepository
              .getRunByIdAndUser(runId, userId)
              .recoverWith {
                case _ => internalError("find run")
              }
              .map(_.filter(_.projectId == projectId))
          }

      def getRunsByProject(projectId: ProjectId, userId: UserId): Future[Seq[Run]] =
        projectService
          .getUserProjectById(projectId, userId)
          .recoverWith {
            case e: ProjectServiceException => serviceErrorMapper(e)
          }
          .flatMap(_ => runRepository.getRunsByProject(projectId).recoverWith { case _ => internalError("find run") })

      def deleteRunById(runId: RunId, projectId: ProjectId, userId: UserId): Future[Int] =
        getRunByIdAndUser(runId, projectId, userId).flatMap {
          case Some(_) => runRepository.deleteRunById(runId).recoverWith { case _ => internalError("delete run") }
          case None    => Future.failed(NotFound())
        }

      def updateRun(runId: RunId, request: RunUpdateRequest, projectId: ProjectId, userId: UserId): Future[Int] =
        getRunByIdAndUser(runId, projectId, userId).flatMap {
          case Some(run) =>
            runRepository
              .updateRun(
                run.copy(
                  status = request.status,
                  timeStart = request.timeStart,
                  timeEnd = request.timeEnd,
                  results = request.results,
                  cmwlWorkflowId = request.cmwlWorkflowId
                )
              )
              .recoverWith { case _ => internalError("update run") }
          case None => Future.failed(NotFound())
        }

      private def internalError(action: String): Future[Nothing] =
        Future.failed(InternalError(s"Failed to $action due to unexpected internal error"))

      private def serviceErrorMapper(exc: ProjectServiceException): Future[Nothing] =
        exc match {
          case _: ProjectService.Exceptions.AccessDenied  => Future.failed(AccessDenied())
          case _: ProjectService.Exceptions.NotFound      => Future.failed(NotFound())
          case e: ProjectService.Exceptions.InternalError => Future.failed(InternalError(e.getMessage))
        }
    }

}
