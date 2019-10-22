package cromwell.pipeline.datastorage.dao.entry

import cromwell.pipeline.database.PipelineDatabaseEngine
import cromwell.pipeline.datastorage.dto._

class ProjectEntry(val pipelineDatabaseEngine: PipelineDatabaseEngine) {
  import pipelineDatabaseEngine.profile.api._

  class Projects(tag: Tag) extends Table[Project](tag, "project") {
    def projectId = column[ProjectId]("project_id", O.PrimaryKey)
    def name = column[String]("name")
    def description = column[String]("description")
    def repository = column[String]("repository")
    def * = (projectId, name, description, repository) <>
      (Project.tupled, Project.unapply)
  }

  val projects = TableQuery[Projects]

  def getProjectByIdAction = Compiled { projectId: Rep[ProjectId] =>
    projects.filter(_.projectId === projectId).take(1)
  }

  def addProjectAction(project: Project) = (projects.returning(projects.map(_.projectId))) += project
}
