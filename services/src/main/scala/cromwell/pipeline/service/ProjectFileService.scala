package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.womtool.WomToolAPI

import scala.concurrent.{ ExecutionContext, Future }

class ProjectFileService(
  projectService: ProjectService,
  womTool: WomToolAPI,
  projectVersioning: ProjectVersioning[VersioningException]
)(
  implicit executionContext: ExecutionContext
) {

  def validateFile(fileContent: ProjectFileContent): Future[Either[ValidationError, Unit]] =
    Future(womTool.validate(fileContent.content)).map {
      case Left(value) => Left(ValidationError(value.toList))
      case Right(_)    => Right(())
    }

  def uploadFile(
    projectId: ProjectId,
    projectFile: ProjectFile,
    version: Option[PipelineVersion],
    userId: UserId
  ): Future[Either[VersioningException, UpdateFiledResponse]] =
    projectService
      .getUserProjectById(projectId, userId)
      .flatMap(project => projectVersioning.updateFile(project, projectFile, version))

  def buildConfiguration(
    projectId: ProjectId,
    projectFile: ProjectFile
  ): Future[Either[ValidationError, ProjectConfiguration]] =
    Future(womTool.inputsToList(projectFile.content.content)).map {
      case Left(value) => Left(ValidationError(value.toList))
      case Right(nodes) =>
        Right(ProjectConfiguration(projectId, active = true, List(ProjectFileConfiguration(projectFile.path, nodes))))
    }
}
