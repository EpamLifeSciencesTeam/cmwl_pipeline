package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dto.{ FileContent, Project, ProjectFile, ValidationError }
import cromwell.pipeline.womtool.WomToolAPI

import scala.concurrent.{ ExecutionContext, Future }

class ProjectFileService(womTool: WomToolAPI, projectVersioning: ProjectVersioning[VersioningException])(
  implicit executionContext: ExecutionContext
) {

  def validateFile(fileContent: FileContent): Future[Either[ValidationError, Unit]] =
    Future(womTool.validate(fileContent.content)).map {
      case Left(value) => Left(ValidationError(value.toList))
      case Right(_)    => Right(())
    }

  def uploadFile(project: Project, projectFile: ProjectFile): Future[Either[VersioningException, String]] =
    projectVersioning.updateFile(project, projectFile)
}
