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
    repository.getByParam("projectId", projectId.value).map(_.headOption.map(fromDocument))
}
