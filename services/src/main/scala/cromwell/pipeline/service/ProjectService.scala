package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.ProjectRepository
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.service.ProjectService.Exceptions.{ ProjectAccessDeniedException, ProjectNotFoundException }

import java.util.UUID
import scala.concurrent.{ ExecutionContext, Future }

trait ProjectService {

  def getUserProjectById(projectId: ProjectId, userId: UserId): Future[Project]

  def getUserProjects(userId: UserId): Future[Seq[Project]]

  def getUserProjectByName(namePattern: String, userId: UserId): Future[Project]

  def addProject(request: ProjectAdditionRequest, userId: UserId): Future[Either[VersioningException, Project]]

  def deactivateProjectById(projectId: ProjectId, userId: UserId): Future[Project]

  def updateProjectName(projectId: ProjectId, request: ProjectUpdateNameRequest, userId: UserId): Future[ProjectId]

  def updateProjectVersion(projectId: ProjectId, version: PipelineVersion, userId: UserId): Future[Int]

}

object ProjectService {

  object Exceptions {
    final case class ProjectNotFoundException(message: String = "Project not found") extends RuntimeException(message)
    final case class ProjectAccessDeniedException(message: String = "Access denied. You  not owner of the project")
        extends RuntimeException(message)
  }

  def apply(projectRepository: ProjectRepository, projectVersioning: ProjectVersioning[VersioningException])(
    implicit executionContext: ExecutionContext
  ): ProjectService =
    new ProjectService {

      def getUserProjectById(projectId: ProjectId, userId: UserId): Future[Project] =
        projectRepository.getProjectById(projectId).flatMap(project => getForUserOrFail(project.toSeq, userId))

      def getUserProjects(userId: UserId): Future[Seq[Project]] =
        projectRepository.getProjectsByOwnerId(userId)

      def getUserProjectByName(namePattern: String, userId: UserId): Future[Project] =
        projectRepository.getProjectsByName(namePattern).flatMap(getForUserOrFail(_, userId))

      private def getForUserOrFail(projects: Seq[Project], userId: UserId): Future[Project] =
        projects.find(_.ownerId == userId) match {
          case Some(project)             => Future.successful(project)
          case None if projects.nonEmpty => Future.failed(new ProjectAccessDeniedException)
          case _                         => Future.failed(new ProjectNotFoundException)
        }

      def addProject(request: ProjectAdditionRequest, userId: UserId): Future[Either[VersioningException, Project]] = {
        val localProject =
          LocalProject(
            projectId = ProjectId(UUID.randomUUID().toString),
            ownerId = userId,
            name = request.name,
            active = true,
            version = projectVersioning.getDefaultProjectVersion()
          )
        projectVersioning.createRepository(localProject).flatMap {
          case Left(exception) => Future.successful(Left(exception))
          case Right(project)  => projectRepository.addProject(project).map(_ => Right(project))
        }
      }

      def deactivateProjectById(projectId: ProjectId, userId: UserId): Future[Project] =
        getUserProjectById(projectId, userId).flatMap { project =>
          if (!project.active) {
            Future.successful(project)
          } else {
            projectRepository.deactivateProjectById(projectId).flatMap(_ => getUserProjectById(projectId, userId))
          }
        }

      def updateProjectName(
        projectId: ProjectId,
        request: ProjectUpdateNameRequest,
        userId: UserId
      ): Future[ProjectId] =
        getUserProjectById(projectId, userId).flatMap { project =>
          val updatedProject = project.copy(name = request.name)
          projectRepository.updateProjectName(updatedProject).map(_ => updatedProject.projectId)
        }

      def updateProjectVersion(projectId: ProjectId, version: PipelineVersion, userId: UserId): Future[Int] =
        getUserProjectById(projectId, userId).flatMap { project =>
          projectRepository.updateProjectVersion(project.copy(version = version))
        }
    }
}
