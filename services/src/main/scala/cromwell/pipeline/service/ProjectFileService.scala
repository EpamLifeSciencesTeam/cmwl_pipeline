package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.womtool.WomToolAPI

import scala.concurrent.{ ExecutionContext, Future }

class ProjectFileService(womTool: WomToolAPI, projectVersioning: ProjectVersioning[VersioningException])(
  implicit executionContext: ExecutionContext
) {

  def validateFile(fileContent: ProjectFileContent): Future[Either[ValidationError, Unit]] =
    Future(womTool.validate(fileContent.content)).map {
      case Left(value) => Left(ValidationError(value.toList))
      case Right(_)    => Right(())
    }

  def uploadFile(
    project: Project,
    projectFile: ProjectFile,
    version: Option[PipelineVersion]
  ): Future[Either[VersioningException, SuccessResponseMessage]] =
    projectVersioning.updateFile(project, projectFile, version)

  def buildConfiguration(
    projectId: ProjectId,
    projectFile: ProjectFile
  ): Future[Either[ValidationError, ProjectConfiguration]] =
    Future(womTool.inputsToList(projectFile.content.content)).map {
      case Left(value) => Left(ValidationError(value.toList))
      case Right(nodes) =>
        Right(ProjectConfiguration(projectId, List(ProjectFileConfiguration(projectFile.path, nodes))))
    }
}
