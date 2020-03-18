package cromwell.pipeline.service

import java.util.UUID

import cromwell.pipeline.datastorage.dao.repository.ProjectRepository
import cromwell.pipeline.datastorage.dto.{Project, ProjectAdditionRequest, ProjectId, ProjectUpdateRequest, UserId}
import cromwell.pipeline.service.Exceptions.{ProjectAccessDeniedException, ProjectNotFoundException}

import scala.concurrent.{ExecutionContext, Future}

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

  def addProject(request: ProjectAdditionRequest, userId: UserId, repoStub: String): Future[ProjectId] = {
    val project =
      Project(
        projectId = ProjectId(UUID.randomUUID().toString),
        ownerId = userId,
        name = request.name,
        repository = repoStub, /* A stub for project repository. The project repository will be appointed later*/
        active = true
      )
    projectRepository.addProject(project)
  }

  def deactivateProjectById(projectId: ProjectId, userId: UserId): Future[Option[Project]] = {
    val result = projectRepository.getProjectById(projectId)
    result.flatMap {
      case Some(project) if project.ownerId != userId => throw new ProjectAccessDeniedException
      case Some(_) =>
        for {
          _ <- projectRepository.deactivateProjectById(projectId)
          getProject <- projectRepository.getProjectById(projectId)
        } yield getProject
      case None => throw new ProjectNotFoundException
    }
  }

  def updateProject(request: ProjectUpdateRequest, userId: UserId): Future[Int] =
    projectRepository
      .getProjectById(request.projectId)
      .flatMap(
        projectOpt =>
          projectOpt.map(
            project =>
              if (project.ownerId == userId)
                projectRepository.updateProject(
                  project.copy(name = request.name, repository = request.repository)
                )
              else Future.failed(new ProjectAccessDeniedException)
          ) match {
            case Some(value) => value
            case None        => Future.failed(new ProjectNotFoundException)
          }
      )


}

object Exceptions {
  case class ProjectNotFoundException(message: String = "Project not found") extends RuntimeException(message)
  case class ProjectAccessDeniedException(message: String = "Access denied. You  not owner of the project") extends RuntimeException(message)
}