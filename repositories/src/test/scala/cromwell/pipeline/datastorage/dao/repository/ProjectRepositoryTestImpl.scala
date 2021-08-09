package cromwell.pipeline.datastorage.dao.repository

import cromwell.pipeline.datastorage.dto.{ Project, ProjectId }
import cromwell.pipeline.model.wrapper.UserId

import scala.collection.mutable
import scala.concurrent.Future

class ProjectRepositoryTestImpl extends ProjectRepository {

  private val projects: mutable.Map[ProjectId, Project] = mutable.Map.empty

  def getProjectById(projectId: ProjectId): Future[Option[Project]] =
    Future.successful(projects.get(projectId))

  def getProjectsByName(name: String): Future[Seq[Project]] =
    Future.successful(projects.values.filter(_.name == name).toSeq)

  def addProject(project: Project): Future[ProjectId] = {
    projects += (project.projectId -> project)
    Future.successful(project.projectId)
  }

  def deactivateProjectById(projectId: ProjectId): Future[Int] = {
    for {
      (id, project) <- projects if id == projectId
      deactivatedProject = project.copy(active = false)
    } yield projects += (deactivatedProject.projectId -> deactivatedProject)

    Future.successful(0)
  }

  def updateProjectName(updatedProject: Project): Future[Int] = updateProject(updatedProject)

  def updateProjectVersion(updatedProject: Project): Future[Int] = updateProject(updatedProject)

  private def updateProject(updatedProject: Project): Future[Int] = {
    if (projects.contains(updatedProject.projectId)) projects += (updatedProject.projectId -> updatedProject)
    Future.successful(0)
  }

  override def getProjectsByOwnerId(userId: UserId): Future[Seq[Project]] =
    Future.successful(projects.values.filter(_.ownerId == userId).toSeq)
}

object ProjectRepositoryTestImpl {

  def apply(projects: Project*): ProjectRepositoryTestImpl = {
    val projectRepositoryTestImpl = new ProjectRepositoryTestImpl
    projects.foreach(projectRepositoryTestImpl.addProject)
    projectRepositoryTestImpl
  }

}
