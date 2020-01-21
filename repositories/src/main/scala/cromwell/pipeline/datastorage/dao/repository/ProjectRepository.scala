package cromwell.pipeline.datastorage.dao.repository

import cromwell.pipeline.database.PipelineDatabaseEngine
import cromwell.pipeline.datastorage.dao.ProjectEntry
import cromwell.pipeline.datastorage.dto.{ Project, ProjectId }

import scala.concurrent.Future

class ProjectRepository(pipelineDatabaseEngine: PipelineDatabaseEngine, projectEntry: ProjectEntry) {

  import pipelineDatabaseEngine._
  import pipelineDatabaseEngine.profile.api._

  def getProjectById(projectId: ProjectId): Future[Option[Project]] =
    database.run(projectEntry.getProjectByIdAction(projectId).result.headOption)

  def getProjectByName(name: String): Future[Option[Project]] =
    database.run(projectEntry.getProjectByName(name).result.headOption)

  def addProject(project: Project): Future[ProjectId] = database.run(projectEntry.addProjectAction(project))

  def deactivateProjectById(projectId: ProjectId): Future[Int] =
    database.run(projectEntry.deactivateProjectById(projectId))

}
