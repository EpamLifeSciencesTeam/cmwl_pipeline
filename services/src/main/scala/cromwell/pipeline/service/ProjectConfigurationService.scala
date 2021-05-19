package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.ProjectConfigurationRepository
import cromwell.pipeline.datastorage.dto.{
  FileParameter,
  ProjectConfiguration,
  ProjectFileConfiguration,
  ProjectId,
  TypedValue
}
import cromwell.pipeline.model.wrapper.UserId

import java.nio.file.Path
import scala.concurrent.{ ExecutionContext, Future }

class ProjectConfigurationService(repository: ProjectConfigurationRepository, projectService: ProjectService)(
  implicit ec: ExecutionContext
) {

  def addConfiguration(projectConfiguration: ProjectConfiguration, userId: UserId): Future[Unit] = {
    def toTuple(config: ProjectFileConfiguration): (Path, Map[String, TypedValue]) =
      config.path -> config.inputs.map(fileParameter => fileParameter.name -> fileParameter.typedValue).toMap

    def toProjectFileConfiguration(path: Path, inputs: Map[String, TypedValue]): ProjectFileConfiguration =
      ProjectFileConfiguration(path, inputs.map((FileParameter.apply _).tupled).toList)

    getConfigurationById(projectConfiguration.projectId, userId).flatMap {
      case Some(config) =>
        val oldFileConfigsMap = config.projectFileConfigurations.map(toTuple).toMap
        val newFileConfigsMap = projectConfiguration.projectFileConfigurations.map(toTuple).toMap

        val updatedFileConfigsMap = oldFileConfigsMap ++ newFileConfigsMap
        val updatedFileConfigs = updatedFileConfigsMap.map((toProjectFileConfiguration _).tupled).toList

        val newConfig = projectConfiguration.copy(projectFileConfigurations = updatedFileConfigs)
        updateConfiguration(newConfig)
      case None =>
        repository.addConfiguration(projectConfiguration)
    }
  }

  def getConfigurationById(projectId: ProjectId, userId: UserId): Future[Option[ProjectConfiguration]] =
    projectService
      .getUserProjectById(projectId, userId)
      .flatMap(_ => repository.getById(projectId).map(_.filter(_.active)))

  def deactivateConfiguration(projectId: ProjectId, userId: UserId): Future[Unit] =
    getConfigurationById(projectId, userId).flatMap {
      case Some(config) => updateConfiguration(config.copy(active = false))
      case _            => Future.failed(new RuntimeException("There is no project to deactivate"))
    }

  private def updateConfiguration(projectConfiguration: ProjectConfiguration): Future[Unit] =
    repository.updateConfiguration(projectConfiguration)
}
