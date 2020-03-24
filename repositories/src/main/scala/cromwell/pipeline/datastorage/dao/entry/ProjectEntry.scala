package cromwell.pipeline.datastorage.dao

import cromwell.pipeline.datastorage.Profile
import cromwell.pipeline.datastorage.dao.entry.UserEntry
import cromwell.pipeline.datastorage.dto.formatters.ProjectFormatters.ProjectId
import cromwell.pipeline.datastorage.dto.{Project, UserId}

trait ProjectEntry {
  this: Profile with UserEntry =>

  import profile.api._

  class ProjectTable(tag: Tag) extends Table[Project](tag, "project") {
    def projectId = column[ProjectId]("project_id", O.PrimaryKey)
    def ownerId = column[UserId]("owner_id")
    def name = column[String]("name")
    def repository = column[String]("repository")
    def active = column[Boolean]("active")
    def * = (projectId, ownerId, name, repository, active) <>
      ((Project.apply _).tupled, Project.unapply)

    def user = foreignKey("fk_project_user", ownerId, users)(_.userId)
  }

  val projects = TableQuery[ProjectTable]

  def getProjectByIdAction = Compiled { projectId: Rep[ProjectId] =>
    projects.filter(_.projectId === projectId).take(1)
  }

  def getProjectByNameAction = Compiled { name: Rep[String] =>
    projects.filter(_.name === name).take(1)
  }

  def addProjectAction(project: Project) = projects.returning(projects.map(_.projectId)) += project

  def deactivateProjectByIdAction(projectId: ProjectId) =
    projects.filter(_.projectId === projectId).map(_.active).update(false)

  def updateProjectAction(updatedProject: Project) =
    projects
      .filter(_.projectId === updatedProject.projectId)
      .map(project => (project.name, project.repository))
      .update((updatedProject.name, updatedProject.repository))

}
