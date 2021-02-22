package cromwell.pipeline.datastorage.dao.repository

import cromwell.pipeline.datastorage.dao.mongo.DocumentCodecInstances.projectConfigurationDocumentCodec
import cromwell.pipeline.datastorage.dao.mongo.DocumentRepository
import cromwell.pipeline.datastorage.dto.{ ProjectConfiguration, ProjectId }

import scala.concurrent.{ ExecutionContext, Future }

class ProjectConfigurationRepository(repository: DocumentRepository)(implicit ec: ExecutionContext) {

  private def upsertConfiguration(projectConfiguration: ProjectConfiguration): Future[Unit] =
    repository.upsertOne(projectConfiguration, "projectId", projectConfiguration.projectId.value)

  def addConfiguration(projectConfiguration: ProjectConfiguration): Future[Unit] =
    upsertConfiguration(projectConfiguration)

  def updateConfiguration(projectConfiguration: ProjectConfiguration): Future[Unit] =
    upsertConfiguration(projectConfiguration)

  def getById(projectId: ProjectId): Future[Option[ProjectConfiguration]] =
    repository.getByParam("projectId", projectId.value).map(_.headOption)
}
