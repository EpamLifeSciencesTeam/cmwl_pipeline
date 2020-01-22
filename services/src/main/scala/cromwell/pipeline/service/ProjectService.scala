package cromwell.pipeline.service

import java.util.UUID

import cromwell.pipeline.datastorage.dao.repository.ProjectRepository
import cromwell.pipeline.datastorage.dto.{ Project, ProjectAdditionRequest, ProjectId }

import scala.concurrent.{ ExecutionContext, Future }

class ProjectService(projectRepository: ProjectRepository)(implicit executionContext: ExecutionContext) {

  def getProjectById(projectId: ProjectId): Future[Option[Project]] =
    projectRepository.getProjectById(projectId)

  def getProjectByName(namePattern: String): Future[Option[Project]] =
    projectRepository.getProjectByName(namePattern)

  def addProject(request: ProjectAdditionRequest): Future[ProjectId] = {
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

  def deactivateProjectById(projectId: ProjectId): Future[Option[Project]] =
    for {
      _ <- projectRepository.deactivateProjectById(projectId)
      getProject <- projectRepository.getProjectById(projectId)
    } yield getProject

}
