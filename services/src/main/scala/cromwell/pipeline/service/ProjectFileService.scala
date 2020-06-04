package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dto.{ FileContent, Project, ProjectFile, ValidationError, Version }
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

  def uploadFile(
    project: Project,
    projectFile: ProjectFile,
    versionName: Option[String]
  ): Future[Either[VersioningException, String]] =
    versionName match {
      case Some(value) =>
        projectVersioning.getFilesVersions(project, projectFile.path).flatMap {
          case Right(versions) => projectVersioning.updateFile(project, projectFile, versions.find(_.name == value))
          case Left(exception) => Future(Left(exception))
        }
      case None => projectVersioning.updateFile(project, projectFile, None)
    }
}
