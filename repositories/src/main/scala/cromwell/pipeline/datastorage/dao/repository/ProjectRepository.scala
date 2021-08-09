package cromwell.pipeline.datastorage.dao.repository

import cromwell.pipeline.database.PipelineDatabaseEngine
import cromwell.pipeline.datastorage.dao.entry.ProjectEntry
import cromwell.pipeline.datastorage.dto.{ Project, ProjectId }
import cromwell.pipeline.model.wrapper.UserId

import scala.concurrent.Future

trait ProjectRepository {

  def getProjectsByOwnerId(userId: UserId): Future[Seq[Project]]

  def getProjectById(projectId: ProjectId): Future[Option[Project]]

  def getProjectsByName(name: String): Future[Seq[Project]]

  def addProject(project: Project): Future[ProjectId]

  def deactivateProjectById(projectId: ProjectId): Future[Int]

  def updateProjectName(updatedProject: Project): Future[Int]

  def updateProjectVersion(updatedProject: Project): Future[Int]

}

object ProjectRepository {

  def apply(pipelineDatabaseEngine: PipelineDatabaseEngine, projectEntry: ProjectEntry): ProjectRepository =
    new ProjectRepository {

      import pipelineDatabaseEngine._

      import pipelineDatabaseEngine.profile.api._

      def getProjectsByOwnerId(userId: UserId): Future[Seq[Project]] =
        database.run(projectEntry.getProjectsByOwnerIdAction(userId).result)

      def getProjectById(projectId: ProjectId): Future[Option[Project]] =
        database.run(projectEntry.getProjectByIdAction(projectId).result.headOption)

      def getProjectsByName(name: String): Future[Seq[Project]] =
        database.run(projectEntry.getProjectsByNameAction(name).result)

      def addProject(project: Project): Future[ProjectId] = database.run(projectEntry.addProjectAction(project))

      def deactivateProjectById(projectId: ProjectId): Future[Int] =
        database.run(projectEntry.deactivateProjectByIdAction(projectId))

      def updateProjectName(updatedProject: Project): Future[Int] =
        database.run(projectEntry.updateProjectNameAction(updatedProject))

      def updateProjectVersion(updatedProject: Project): Future[Int] =
        database.run(projectEntry.updateProjectVersionAction(updatedProject))

    }

}
