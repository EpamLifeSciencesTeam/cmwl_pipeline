package cromwell.pipeline.datastorage.dao.repository

import cromwell.pipeline.datastorage.dao.mongo.DocumentCodecInstances.projectConfigurationDocumentCodec
import cromwell.pipeline.datastorage.dao.mongo.DocumentRepository
import cromwell.pipeline.datastorage.dto.{ ProjectConfiguration, ProjectConfigurationId, ProjectId }

import scala.concurrent.{ ExecutionContext, Future }

class ProjectConfigurationRepository(repository: DocumentRepository)(implicit ec: ExecutionContext) {

  private def upsertConfiguration(projectConfiguration: ProjectConfiguration): Future[Unit] =
    repository.upsertOne(projectConfiguration, "id", projectConfiguration.id.value)

  def addConfiguration(projectConfiguration: ProjectConfiguration): Future[Unit] =
    upsertConfiguration(projectConfiguration)

  def updateConfiguration(projectConfiguration: ProjectConfiguration): Future[Unit] =
    upsertConfiguration(projectConfiguration)

  def getById(id: ProjectConfigurationId): Future[Option[ProjectConfiguration]] =
    repository.getByParam("id", id.value).map(_.headOption)

  def getAllByProjectId(projectId: ProjectId): Future[Seq[ProjectConfiguration]] =
    repository.getByParam("projectId", projectId.value)
}
