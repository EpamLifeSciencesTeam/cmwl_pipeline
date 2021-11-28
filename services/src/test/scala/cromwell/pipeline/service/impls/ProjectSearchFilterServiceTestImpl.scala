package cromwell.pipeline.service.impls

import cromwell.pipeline.datastorage.dto.{ ProjectSearchFilter, ProjectSearchQuery }
import cromwell.pipeline.model.wrapper.ProjectSearchFilterId
import cromwell.pipeline.service.ProjectSearchFilterService

import java.time.Instant
import scala.concurrent.Future

class ProjectSearchFilterServiceTestImpl(projectSearchFilters: Seq[ProjectSearchFilter], testMode: TestMode)
    extends ProjectSearchFilterService {

  def addProjectSearchFilter(query: ProjectSearchQuery): Future[ProjectSearchFilterId] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(ProjectSearchFilterId.random)
    }

  def getSearchFilterById(filterId: ProjectSearchFilterId): Future[ProjectSearchFilter] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _ =>
        projectSearchFilters.headOption match {
          case Some(filter) => Future.successful(filter)
          case None         => Future.failed(ProjectSearchFilterService.Exceptions.NotFound())
        }
    }

  def updateLastUsedAt(filterId: ProjectSearchFilterId, lastUsedAt: Instant): Future[Unit] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.unit
    }

  def deleteOldFilters(usedLaterThan: Instant): Future[Unit] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.unit
    }

}

object ProjectSearchFilterServiceTestImpl {

  def apply(filters: ProjectSearchFilter*): ProjectSearchFilterServiceTestImpl =
    new ProjectSearchFilterServiceTestImpl(projectSearchFilters = filters, testMode = Success)

  def withException(exception: Throwable): ProjectSearchFilterServiceTestImpl =
    new ProjectSearchFilterServiceTestImpl(projectSearchFilters = Seq.empty, testMode = WithException(exception))

}
