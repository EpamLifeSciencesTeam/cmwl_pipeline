package cromwell.pipeline.datastorage.dao.entry

import cromwell.pipeline.datastorage.Profile
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.ProjectSearchFilterId
import slick.lifted.ProvenShape

import java.time.Instant

trait ProjectSearchFilterEntry { this: Profile with MyPostgresProfile with AliasesSupport =>
  import Implicits._
  import api._

  class ProjectSearchFilterTable(tag: Tag) extends Table[ProjectSearchFilter](tag, "project_search_filter") {
    def filterId: Rep[ProjectSearchFilterId] = column[ProjectSearchFilterId]("filter_id", O.PrimaryKey)
    def query: Rep[ProjectSearchQuery] = column[ProjectSearchQuery]("query")
    def lastUsedAt: Rep[Instant] = column[Instant]("last_used_at")
    override def * : ProvenShape[ProjectSearchFilter] =
      (filterId, query, lastUsedAt) <> ((ProjectSearchFilter.apply _).tupled, ProjectSearchFilter.unapply)
  }

  val projectFilters = TableQuery[ProjectSearchFilterTable]

  def getFilterById = Compiled { filterId: Rep[ProjectSearchFilterId] =>
    projectFilters.filter(_.filterId === filterId).take(1)
  }

  def addFilter(projectSearchFilter: ProjectSearchFilter): ActionResult[ProjectSearchFilterId] =
    projectFilters.returning(projectFilters.map(_.filterId)) += projectSearchFilter

  def updateLastUsedAt(filterId: ProjectSearchFilterId, lastUsedAt: Instant): ActionResult[Int] =
    projectFilters.filter(_.filterId === filterId).map(searchFilter => searchFilter.lastUsedAt).update(lastUsedAt)

  def deleteOldFilters(usedLaterThan: Instant): ActionResult[Int] =
    projectFilters.filter(_.lastUsedAt <= usedLaterThan).delete
}
