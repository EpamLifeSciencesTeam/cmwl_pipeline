package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.ProjectSearchFilterRepository
import cromwell.pipeline.datastorage.dto.{ ProjectSearchFilter, ProjectSearchQuery }
import cromwell.pipeline.model.wrapper.ProjectSearchFilterId
import cromwell.pipeline.service.ProjectSearchFilterService.Exceptions._
import cromwell.pipeline.service.exceptions.ServiceException

import java.time.Instant
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Success

trait ProjectSearchFilterService {

  def addProjectSearchFilter(filter: ProjectSearchQuery): Future[ProjectSearchFilterId]

  def getSearchFilterById(searchId: ProjectSearchFilterId): Future[ProjectSearchFilter]

  def updateLastUsedAt(searchId: ProjectSearchFilterId, lastUsedAt: Instant): Future[Unit]

  def deleteOldFilters(usedLaterThan: Instant): Future[Unit]

}

object ProjectSearchFilterService {

  object Exceptions {
    sealed abstract class ProjectSearchFilterServiceException(message: String) extends ServiceException(message)
    final case class NotFound(message: String = "Filter with that Id does not exist")
        extends ProjectSearchFilterServiceException(message)
    final case class InternalError(message: String = "Internal error")
        extends ProjectSearchFilterServiceException(message)
  }

  def apply(projectSearchFilterRepository: ProjectSearchFilterRepository)(
    implicit executionContext: ExecutionContext
  ): ProjectSearchFilterService =
    new ProjectSearchFilterService {

      override def addProjectSearchFilter(query: ProjectSearchQuery): Future[ProjectSearchFilterId] = {
        val projectSearchFilter = ProjectSearchFilter(
          id = ProjectSearchFilterId.random,
          query = query,
          lastUsedAt = Instant.now
        )
        projectSearchFilterRepository.addProjectSearchFilter(projectSearchFilter).recoverWith {
          case _ => internalError("add filter")
        }
      }

      override def getSearchFilterById(filterId: ProjectSearchFilterId): Future[ProjectSearchFilter] =
        projectSearchFilterRepository.getProjectSearchFilterById(filterId).transformWith {
          case Success(Some(projectFilter)) => Future.successful(projectFilter)
          case Success(None)                => Future.failed(NotFound())
          case _                            => internalError("find filter")
        }

      override def updateLastUsedAt(searchId: ProjectSearchFilterId, lastUsedAt: Instant): Future[Unit] =
        projectSearchFilterRepository.updateLastUsedAt(searchId, lastUsedAt).transformWith {
          case Success(_) => Future.unit
          case _          => internalError("update lastUsedAt")
        }

      override def deleteOldFilters(usedLaterThan: Instant): Future[Unit] =
        projectSearchFilterRepository.deleteOldFilters(usedLaterThan).transformWith {
          case Success(_) => Future.unit
          case _          => internalError("delete old filters")
        }

      private def internalError(action: String): Future[Nothing] =
        Future.failed(InternalError(s"Failed to $action due to unexpected internal error"))

    }

}
