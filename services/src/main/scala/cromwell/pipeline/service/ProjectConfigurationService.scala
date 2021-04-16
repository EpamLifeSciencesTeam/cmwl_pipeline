package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.DocumentRepository
import cromwell.pipeline.datastorage.dto.{
  FileParameter,
  ProjectConfiguration,
  ProjectFileConfiguration,
  ProjectId,
  TypedValue
}

import java.nio.file.Path
import scala.concurrent.{ ExecutionContext, Future }

class ProjectConfigurationService(repository: DocumentRepository)(implicit ec: ExecutionContext) {
  import ProjectConfiguration._

  def addConfiguration(projectConfiguration: ProjectConfiguration): Future[String] = {
    def updateConfiguration(configuration: ProjectConfiguration) =
      repository.updateOne(toDocument(configuration), "projectId", projectConfiguration.projectId.value).map(_.toString)

    def toTuple(config: ProjectFileConfiguration): (Path, Map[String, TypedValue]) =
      config.path -> config.inputs.map(fileParameter => fileParameter.name -> fileParameter.typedValue).toMap

    def toProjectFileConfiguration(path: Path, inputs: Map[String, TypedValue]): ProjectFileConfiguration =
      ProjectFileConfiguration(path, inputs.map((FileParameter.apply _).tupled).toList)

    getById(projectConfiguration.projectId).flatMap {
      case Some(config) =>
        val oldFileConfigsMap = config.projectFileConfigurations.map(toTuple).toMap
        val newFileConfigsMap = projectConfiguration.projectFileConfigurations.map(toTuple).toMap

        val updatedFileConfigsMap = oldFileConfigsMap ++ newFileConfigsMap
        val updatedFileConfigs = updatedFileConfigsMap.map((toProjectFileConfiguration _).tupled).toList

        val newConfig = projectConfiguration.copy(projectFileConfigurations = updatedFileConfigs)
        updateConfiguration(newConfig)
      case None =>
        updateConfiguration(projectConfiguration)
    }
  }

  def getById(projectId: ProjectId): Future[Option[ProjectConfiguration]] =
    repository.getByParam("projectId", projectId.value).map(_.headOption.map(fromDocument).filter(_.active))

  def deactivateConfiguration(projectId: ProjectId): Future[String] =
    getById(projectId).flatMap {
      case Some(config) =>
        repository.updateOne(toDocument(config.copy(active = false)), "projectId", projectId.value).map(_.toString)
      case _ => Future.failed(new RuntimeException("There is no project to deactivate"))
    }
}
