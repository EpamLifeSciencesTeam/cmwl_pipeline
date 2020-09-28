package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.DocumentRepository
import cromwell.pipeline.datastorage.dto.project.configuration.ProjectConfigurationEntity
import cromwell.pipeline.datastorage.dto.{ ProjectConfiguration, ProjectId }

import scala.concurrent.{ ExecutionContext, Future }

class ProjectConfigurationService(repository: DocumentRepository)(implicit ec: ExecutionContext) {
  import ProjectConfiguration._

  def addConfiguration(configuration: ProjectConfigurationEntity): Future[String] =
    repository
      .replaceOne(
        toDocument(ProjectConfiguration(configuration)),
        "projectId",
        configuration.projectId.value
      )
      .map(_.toString)

  def getById(projectId: ProjectId): Future[Option[ProjectConfigurationEntity]] =
    repository.getByParam("projectId", projectId.value).map { optDocument =>
      optDocument.filter(document => fromDocument(document).isActive).map { document =>
        val config = fromDocument(document)
        ProjectConfigurationEntity(config.projectId, config.projectFileConfigurations)
      }
    }

  def deactivateConfiguration(projectId: ProjectId): Future[String] =
    getById(projectId).flatMap {
      case Some(_) =>
        repository.updateOneField("projectId", projectId.value, "isActive", false).map(_.toString)
      case _ => Future.failed(new RuntimeException("There is no project to deactivate"))
    }
}
