package cromwell.pipeline.datastorage.dao.repository.impls

import cromwell.pipeline.datastorage.dao.repository.ProjectSearchFilterRepository
import cromwell.pipeline.datastorage.dto.ProjectSearchFilter
import cromwell.pipeline.model.wrapper.ProjectSearchFilterId

import java.time.Instant
import scala.collection.mutable
import scala.concurrent.Future

class ProjectSearchFilterRepositoryTestImpl extends ProjectSearchFilterRepository {

  private val searches: mutable.Map[ProjectSearchFilterId, ProjectSearchFilter] = mutable.Map.empty

  def getProjectSearchFilterById(projectSearchFilterId: ProjectSearchFilterId): Future[Option[ProjectSearchFilter]] =
    Future.successful(searches.get(projectSearchFilterId))

  def addProjectSearchFilter(search: ProjectSearchFilter): Future[ProjectSearchFilterId] = {
    searches += (search.id -> search)
    Future.successful(search.id)
  }

  def updateLastUsedAt(projectSearchId: ProjectSearchFilterId, timestart: Instant): Future[Int] =
    Future.successful(1)

  def deleteOldFilters(usedLaterThan: Instant): Future[Int] =
    Future.successful(1)
}

object ProjectSearchFilterRepositoryTestImpl {

  def apply(searches: ProjectSearchFilter*): ProjectSearchFilterRepositoryTestImpl = {
    val projectSearchRepositoryTestImpl = new ProjectSearchFilterRepositoryTestImpl
    searches.foreach(projectSearchRepositoryTestImpl.addProjectSearchFilter)
    projectSearchRepositoryTestImpl
  }

  def withException(exception: Throwable): ProjectSearchFilterRepository =
    new ProjectSearchFilterRepository {

      override def addProjectSearchFilter(projectSearch: ProjectSearchFilter): Future[ProjectSearchFilterId] =
        Future.failed(exception)

      override def getProjectSearchFilterById(
        projectSearchFilterId: ProjectSearchFilterId
      ): Future[Option[ProjectSearchFilter]] =
        Future.failed(exception)

      override def updateLastUsedAt(projectSearchId: ProjectSearchFilterId, timestart: Instant): Future[Int] =
        Future.failed(exception)

      override def deleteOldFilters(usedLaterThan: Instant): Future[Int] =
        Future.failed(exception)
    }

}
