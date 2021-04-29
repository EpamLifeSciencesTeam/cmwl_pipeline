package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.womtool.WomToolAPI

import java.nio.file.Path
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
    projectFilePath: Path,
    version: Option[PipelineVersion],
    userId: UserId
  ): Future[ProjectConfiguration] = {

    val eitherFile = for {
      project <- projectService.getUserProjectById(projectId, userId)
      eitherFile <- projectVersioning.getFile(project, projectFilePath, version)
    } yield eitherFile

    eitherFile.flatMap {
      case Right(file) =>
        womTool.inputsToList(file.content.content) match {
          case Right(nodes) =>
            Future.successful(
              ProjectConfiguration(projectId, active = true, List(ProjectFileConfiguration(file.path, nodes)))
            )
          case Left(e) => Future.failed(ValidationError(e.toList))
        }
      case Left(versioningException) => Future.failed(versioningException)
    }
  }
}
