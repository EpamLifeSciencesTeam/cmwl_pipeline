package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.{ ProjectSearchFilterId, UserId }
import cromwell.pipeline.service.ProjectSearchService.Exceptions._
import cromwell.pipeline.service.exceptions.ServiceException

import java.time.Instant
import scala.concurrent.{ ExecutionContext, Future }

trait ProjectSearchService {

  def searchProjectsByFilterId(filterId: ProjectSearchFilterId, userId: UserId): Future[ProjectSearchResponse]

  def searchProjectsByNewQuery(request: ProjectSearchRequest, userId: UserId): Future[ProjectSearchResponse]

}

object ProjectSearchService {

  object Exceptions {
    sealed abstract class ProjectSearchException(message: String) extends ServiceException(message)
    final case class NotFound(message: String = "Filter not found") extends ProjectSearchException(message)
    final case class InternalError(message: String = "Internal error") extends ProjectSearchException(message)
  }

  def apply(
    projectService: ProjectService,
    projectSearchFilterService: ProjectSearchFilterService,
    searchEngine: ProjectSearchEngine
  )(
    implicit ec: ExecutionContext
  ): ProjectSearchService =
    new ProjectSearchService {

      override def searchProjectsByFilterId(
        filterId: ProjectSearchFilterId,
        userId: UserId
      ): Future[ProjectSearchResponse] =
        for {
          projectSearchFilter <- getSearchFilterById(filterId)
          _ <- updateProjectSearchFilterLastUsedAt(filterId)
          projects <- searchProjects(projectSearchFilter.query, userId)
        } yield ProjectSearchResponse(filterId, projects)

      override def searchProjectsByNewQuery(
        request: ProjectSearchRequest,
        userId: UserId
      ): Future[ProjectSearchResponse] =
        for {
          searchId <- addProjectSearchFilter(request.filter)
          projects <- searchProjects(request.filter, userId)
        } yield ProjectSearchResponse(searchId, projects)

      private def searchProjects(query: ProjectSearchQuery, userId: UserId): Future[Seq[Project]] =
        searchEngine.searchProjects(query, userId).recoverWith {
          case _ => internalError("process search query")
        }

      private def updateProjectSearchFilterLastUsedAt(filterId: ProjectSearchFilterId): Future[Unit] = {
        val currentTime = Instant.now
        projectSearchFilterService.updateLastUsedAt(filterId, currentTime).recoverWith {
          case _ => internalError("update lastUsedAt")
        }
      }

      private def addProjectSearchFilter(query: ProjectSearchQuery): Future[ProjectSearchFilterId] =
        projectSearchFilterService.addProjectSearchFilter(query).recoverWith {
          case _ => internalError("save filter")
        }

      private def getSearchFilterById(searchId: ProjectSearchFilterId): Future[ProjectSearchFilter] =
        projectSearchFilterService.getSearchFilterById(searchId).recoverWith {
          case ProjectSearchFilterService.Exceptions.NotFound(_) => Future.failed(NotFound())
          case _                                                 => internalError("fetch filter")
        }

      private def internalError(action: String): Future[Nothing] =
        Future.failed(InternalError(s"Failed to $action due to unexpected internal error"))

    }

}
