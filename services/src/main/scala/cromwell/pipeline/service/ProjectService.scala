package cromwell.pipeline.service

import java.util.UUID

import cromwell.pipeline.datastorage.dao.repository.ProjectRepository
import cromwell.pipeline.datastorage.dto.project.ProjectAdditionRequest
import cromwell.pipeline.datastorage.dto.{ Project, ProjectId, UserId }

import scala.concurrent.{ ExecutionContext, Future }

class ProjectService(projectRepository: ProjectRepository)(implicit executionContext: ExecutionContext) {

  def getProjectById(projectId: ProjectId): Future[Option[Project]] =
    projectRepository.getProjectById(projectId)

  def getProjectByName(namePattern: String, userId: UserId): Future[Option[Project]] = {
    val result = projectRepository.getProjectByName(namePattern)
    result.flatMap {
      case Some(project) if project.ownerId == userId => result
      case Some(_)                                    => Future.failed(new ProjectAccessDeniedException)
      case None                                       => Future.failed(new ProjectNotFoundException)
    }
  }

  def addProject(request: ProjectAdditionRequest, userId: UserId): Future[ProjectId] = {
    val project =
      Project(
        projectId = ProjectId(UUID.randomUUID().toString),
        ownerId = userId,
        name = request.name,
        repository = "test_repo",
        active = true
      )
    projectRepository.addProject(project)
  }

  def deactivateProjectById(projectId: ProjectId, userId: UserId): Future[Option[Project]] =
    projectRepository.getProjectById(projectId).flatMap {
      case Some(project) if project.ownerId != userId =>
        throw new ProjectDeactivationForbiddenException
      case Some(_) =>
        for {
          _ <- projectRepository.deactivateProjectById(projectId)
          getProject <- projectRepository.getProjectById(projectId)
        } yield getProject
      case None => throw new ProjectNotFoundException
      case _    => throw new RuntimeException
    }

}

class ProjectNotFoundException extends RuntimeException
class ProjectDeactivationForbiddenException extends RuntimeException
class ProjectAccessDeniedException extends RuntimeException
