package cromwell.pipeline.datastorage.dao.repository.impls

import cromwell.pipeline.datastorage.dao.repository.ProjectConfigurationRepository
import cromwell.pipeline.datastorage.dto.{ ProjectConfiguration, ProjectConfigurationId, ProjectId }

import scala.collection.mutable
import scala.concurrent.Future

class ProjectConfigurationRepositoryTestImpl extends ProjectConfigurationRepository {

  private val configurations: mutable.Map[ProjectConfigurationId, ProjectConfiguration] = mutable.Map.empty

  def addConfiguration(projectConfiguration: ProjectConfiguration): Future[Unit] = {
    configurations += (projectConfiguration.id -> projectConfiguration)
    Future.unit
  }

  def updateConfiguration(projectConfiguration: ProjectConfiguration): Future[Unit] = {
    if (configurations.contains(projectConfiguration.id)) {
      configurations += (projectConfiguration.id -> projectConfiguration)
    }
    Future.unit
  }

  def getById(id: ProjectConfigurationId): Future[Option[ProjectConfiguration]] =
    Future.successful(configurations.get(id))

  def getAllByProjectId(projectId: ProjectId): Future[Seq[ProjectConfiguration]] =
    Future.successful(configurations.values.filter(_.projectId == projectId).toSeq)

}

object ProjectConfigurationRepositoryTestImpl {

  def apply(configurations: ProjectConfiguration*): ProjectConfigurationRepositoryTestImpl = {
    val projectConfigurationRepositoryTestImpl = new ProjectConfigurationRepositoryTestImpl
    configurations.foreach(projectConfigurationRepositoryTestImpl.addConfiguration)
    projectConfigurationRepositoryTestImpl
  }

}
