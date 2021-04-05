package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.DocumentRepository
import cromwell.pipeline.datastorage.dto.{ ProjectConfiguration, ProjectId }

import scala.concurrent.{ ExecutionContext, Future }

class ProjectConfigurationService(repository: DocumentRepository)(implicit ec: ExecutionContext) {
  import ProjectConfiguration._

  def addConfiguration(projectConfiguration: ProjectConfiguration): Future[String] =
    repository
      .updateOne(toDocument(projectConfiguration), "projectId", projectConfiguration.projectId.value)
      .map(_.toString)

  def getById(projectId: ProjectId): Future[Option[ProjectConfiguration]] =
    repository.getByParam("projectId", projectId.value).map(_.headOption.map(fromDocument).filter(_.active))

  def deactivateConfiguration(projectId: ProjectId): Future[String] =
    getById(projectId).flatMap {
      case Some(config) =>
        repository.updateOne(toDocument(config.copy(active = false)), "projectId", projectId.value).map(_.toString)
      case _ => Future.failed(new RuntimeException("There is no project to deactivate"))
    }
}
