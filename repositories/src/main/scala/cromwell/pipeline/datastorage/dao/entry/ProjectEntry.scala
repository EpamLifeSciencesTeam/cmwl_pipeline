package cromwell.pipeline.datastorage.dao.entry

import cromwell.pipeline.datastorage.Profile
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import slick.lifted.MappedToBase.mappedToIsomorphism
import slick.lifted.{ ForeignKeyQuery, ProvenShape }

trait ProjectEntry { this: Profile with UserEntry with CustomsWithEnumSupport with AliasesSupport =>
  import Implicits._
  import api._

  class ProjectTable(tag: Tag) extends Table[Project](tag, "project") {
    def projectId: Rep[ProjectId] = column[ProjectId]("project_id", O.PrimaryKey)
    def ownerId: Rep[UserId] = column[UserId]("owner_id")
    def name: Rep[String] = column[String]("name")
    def active: Rep[Boolean] = column[Boolean]("active")
    def repositoryId: Rep[RepositoryId] = column[RepositoryId]("repository_id")
    def visibility: Rep[Visibility] = column[Visibility]("visibility")
    def version: Rep[PipelineVersion] = column[PipelineVersion]("version")
    def * : ProvenShape[Project] =
      (projectId, ownerId, name, active, repositoryId, version, visibility) <>
        ((Project.apply _).tupled, Project.unapply)

    def user: ForeignKeyQuery[UserTable, UserWithCredentials] = foreignKey("fk_project_user", ownerId, users)(_.userId)
  }

  val projects = TableQuery[ProjectTable]

  def getProjectsByOwnerIdAction = Compiled { userId: Rep[UserId] =>
    projects.filter(_.ownerId === userId)
  }

  def getProjectByIdAction = Compiled { projectId: Rep[ProjectId] =>
    projects.filter(_.projectId === projectId).take(1)
  }

  def getProjectsByNameAction = Compiled { name: Rep[String] =>
    projects.filter(_.name === name)
  }

  def addProjectAction(project: Project): ActionResult[ProjectId] =
    projects.returning(projects.map(_.projectId)) += project

  def deactivateProjectByIdAction(projectId: ProjectId): ActionResult[Int] =
    projects.filter(_.projectId === projectId).map(_.active).update(false)

  def updateProjectNameAction(updatedProject: Project): ActionResult[Int] =
    projects.filter(_.projectId === updatedProject.projectId).map(project => project.name).update(updatedProject.name)

  def updateProjectVersionAction(updatedProject: Project): ActionResult[Int] =
    projects.filter(_.projectId === updatedProject.projectId).map(_.version).update(updatedProject.version)
}
