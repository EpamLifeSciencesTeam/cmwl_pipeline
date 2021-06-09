package cromwell.pipeline.service

import java.util.UUID

import cromwell.pipeline.datastorage.dao.repository.ProjectRepository
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.service.Exceptions.{ ProjectAccessDeniedException, ProjectNotFoundException }

import scala.concurrent.{ ExecutionContext, Future }

class ProjectService(projectRepository: ProjectRepository, projectVersioning: ProjectVersioning[VersioningException])(
  implicit executionContext: ExecutionContext
) {

  private[service] def getUserProjectById(projectId: ProjectId, userId: UserId): Future[Project] =
    projectRepository.getProjectById(projectId).flatMap(getForUserOrFail(_, userId))

  def getUserProjectByName(namePattern: String, userId: UserId): Future[Project] =
    projectRepository.getProjectByName(namePattern).flatMap(getForUserOrFail(_, userId))

  private def getForUserOrFail(project: Option[Project], userId: UserId): Future[Project] =
    project match {
      case Some(project) if project.ownerId == userId => Future.successful(project)
      case Some(_)                                    => Future.failed(new ProjectAccessDeniedException)
      case None                                       => Future.failed(new ProjectNotFoundException)
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

  def updateProjectName(request: ProjectUpdateNameRequest, userId: UserId): Future[ProjectId] =
    getUserProjectById(request.projectId, userId).flatMap { project =>
      val updatedProject = project.copy(name = request.name)
      projectRepository.updateProjectName(updatedProject).map(_ => updatedProject.projectId)
    }

  def updateProjectVersion(projectId: ProjectId, version: PipelineVersion, userId: UserId): Future[Int] =
    getUserProjectById(projectId, userId).flatMap { project =>
      projectRepository.updateProjectVersion(project.copy(version = version))
    }
}

object Exceptions {
  final case class ProjectNotFoundException(message: String = "Project not found") extends RuntimeException(message)
  final case class ProjectAccessDeniedException(message: String = "Access denied. You  not owner of the project")
      extends RuntimeException(message)
}
