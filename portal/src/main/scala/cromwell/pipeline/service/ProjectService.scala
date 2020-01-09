package cromwell.pipeline.service

import java.util.UUID

import cromwell.pipeline.datastorage.dao.repository.ProjectRepository
import cromwell.pipeline.datastorage.dto.{ Project, ProjectCreationRequest, ProjectId, UserId }

import scala.concurrent.{ ExecutionContext, Future }

class ProjectService(projectRepository: ProjectRepository)(implicit executionContext: ExecutionContext) {

  def getProjectById(projectId: ProjectId): Future[Option[Project]] =
    projectRepository.getProjectById(projectId)

  def addProject(request: ProjectCreationRequest): Future[ProjectId] = {
    val project =
      Project(
        projectId = ProjectId(UUID.randomUUID().toString),
        ownerId = request.ownerId,
        name = request.name,
        repository = request.repository,
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
