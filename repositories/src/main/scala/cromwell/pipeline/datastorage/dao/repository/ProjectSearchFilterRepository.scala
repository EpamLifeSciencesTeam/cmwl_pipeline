package cromwell.pipeline.datastorage.dao.repository

import cromwell.pipeline.database.PipelineDatabaseEngine
import cromwell.pipeline.datastorage.dao.entry.ProjectSearchFilterEntry
import cromwell.pipeline.datastorage.dto.ProjectSearchFilter
import cromwell.pipeline.model.wrapper.ProjectSearchFilterId

import java.time.Instant
import scala.concurrent.Future

trait ProjectSearchFilterRepository {

  def addProjectSearchFilter(projectSearchFilter: ProjectSearchFilter): Future[ProjectSearchFilterId]

  def getProjectSearchFilterById(projectSearchFilterId: ProjectSearchFilterId): Future[Option[ProjectSearchFilter]]

  def updateLastUsedAt(projectSearchId: ProjectSearchFilterId, lastUsedAt: Instant): Future[Int]

  def deleteOldFilters(usedLaterThan: Instant): Future[Int]
}

object ProjectSearchFilterRepository {

  def apply(
    pipelineDatabaseEngine: PipelineDatabaseEngine,
    projectSearchFilterEntry: ProjectSearchFilterEntry
  ): ProjectSearchFilterRepository =
    new ProjectSearchFilterRepository {

      import pipelineDatabaseEngine._
      import pipelineDatabaseEngine.profile.api._

      override def addProjectSearchFilter(projectSearchFilter: ProjectSearchFilter): Future[ProjectSearchFilterId] =
        database.run(projectSearchFilterEntry.addFilter(projectSearchFilter))

      override def getProjectSearchFilterById(
        projectSearchFilterId: ProjectSearchFilterId
      ): Future[Option[ProjectSearchFilter]] =
        database.run(projectSearchFilterEntry.getFilterById(projectSearchFilterId).result.headOption)

      override def updateLastUsedAt(projectSearchId: ProjectSearchFilterId, lastUsedAt: Instant): Future[Int] =
        database.run(projectSearchFilterEntry.updateLastUsedAt(projectSearchId, lastUsedAt))

      override def deleteOldFilters(usedLaterThan: Instant): Future[Int] =
        database.run(projectSearchFilterEntry.deleteOldFilters(usedLaterThan))
    }

}
