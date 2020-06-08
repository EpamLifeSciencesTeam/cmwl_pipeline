package cromwell.pipeline.datastorage.dao

import com.github.tminglei.slickpg.PgEnumSupport
import cromwell.pipeline.datastorage.Profile
import cromwell.pipeline.datastorage.dao.entry.UserEntry
import cromwell.pipeline.datastorage.dto.{ Project, ProjectId, Repository, Visibility }
import cromwell.pipeline.model.wrapper.UserId
import slick.basic.Capability
import slick.jdbc.{ JdbcType, PostgresProfile }

trait ProjectProfileWithEnumSupport extends PostgresProfile with PgEnumSupport {
  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + slick.jdbc.JdbcCapabilities.insertOrUpdate
  override val api: API = new API {}
  trait API extends super.API {
    implicit val visibilityTypeMapper: JdbcType[Visibility] =
      createEnumJdbcType[Visibility]("visibility_type", Visibility.toString, Visibility.fromString, quoteName = false)
    implicit val visibilityTypeListMapper: JdbcType[List[Visibility]] =
      createEnumListJdbcType[Visibility](
        "visibility_type",
        Visibility.toString,
        Visibility.fromString,
        quoteName = false
      )
    implicit val visibilityColumnExtensionMethodsBuilder
      : api.Rep[Visibility] => EnumColumnExtensionMethods[Visibility, Visibility] =
      createEnumColumnExtensionMethodsBuilder[Visibility]
    implicit val visibilityOptionColumnExtensionMethodsBuilder
      : api.Rep[Option[Visibility]] => EnumColumnExtensionMethods[Visibility, Option[Visibility]] =
      createEnumOptionColumnExtensionMethodsBuilder[Visibility]
  }
}

trait ProjectEntry {
  this: Profile with UserEntry with ProjectProfileWithEnumSupport =>

  import Implicits._
  import api._

  class ProjectTable(tag: Tag) extends Table[Project](tag, "project") {
    def projectId = column[ProjectId]("project_id", O.PrimaryKey)
    def ownerId = column[UserId]("owner_id")
    def name = column[String]("name")
    def active = column[Boolean]("active")
    def repository = column[Repository]("repository")
    def visibility = column[Visibility]("visibility")
    def * = (projectId, ownerId, name, active, repository.?, visibility) <>
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
      .map(project => (project.name, project.repository.?))
      .update((updatedProject.name, updatedProject.repository))
}
