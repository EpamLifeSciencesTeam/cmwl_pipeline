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

  def deactivateProjectById(projectId: ProjectId): Future[Option[Project]] =
    for {
      _ <- projectRepository.deactivateProjectById(projectId)
      getProject <- projectRepository.getProjectById(projectId)
    } yield getProject

}

case class ProjectNotFoundException(private val message: String = "Project not found") extends RuntimeException(message)
case class ProjectAccessDeniedException(private val message: String = "Access denied") extends RuntimeException(message)
