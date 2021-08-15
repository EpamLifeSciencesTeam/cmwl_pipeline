package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.womtool.WomToolAPI

import java.nio.file.Path
import scala.concurrent.{ ExecutionContext, Future }

trait ProjectFileService {

  def validateFile(fileContent: ProjectFileContent): Future[Either[ValidationError, Unit]]

  def uploadFile(
    projectId: ProjectId,
    projectFile: ProjectFile,
    version: Option[PipelineVersion],
    userId: UserId
  ): Future[Either[VersioningException, Unit]]

  def getFile(
    projectId: ProjectId,
    path: Path,
    version: Option[PipelineVersion] = None,
    userId: UserId
  ): Future[ProjectFile]

  def getFiles(
    projectId: ProjectId,
    version: Option[PipelineVersion] = None,
    userId: UserId
  ): Future[List[ProjectFile]]

}

object ProjectFileService {

  def apply(
    projectService: ProjectService,
    projectConfigurationService: ProjectConfigurationService,
    womTool: WomToolAPI,
    projectVersioning: ProjectVersioning[VersioningException]
  )(
    implicit executionContext: ExecutionContext
  ): ProjectFileService = new ProjectFileService {

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
    ): Future[Either[VersioningException, Unit]] =
      projectService.getUserProjectById(projectId, userId).flatMap { project =>
        projectVersioning.updateFile(project, projectFile, version).flatMap {
          case Left(versioningException) => Future.successful(Left(versioningException))
          case Right(newVersion) =>
            projectService.updateProjectVersion(projectId, newVersion, userId).map(_ => Right(()))
        }
      }

    def getFile(
      projectId: ProjectId,
      path: Path,
      version: Option[PipelineVersion] = None,
      userId: UserId
    ): Future[ProjectFile] =
      projectService.getUserProjectById(projectId, userId).flatMap { project =>
        projectVersioning.getFile(project, path, version).flatMap {
          case Right(projectFile)        => Future.successful(projectFile)
          case Left(versioningException) => Future.failed(versioningException)
        }
      }

    def getFiles(
      projectId: ProjectId,
      version: Option[PipelineVersion] = None,
      userId: UserId
    ): Future[List[ProjectFile]] =
      projectService.getUserProjectById(projectId, userId).flatMap { project =>
        projectVersioning.getFiles(project, version).flatMap {
          case Left(versioningException) => Future.failed(versioningException)
          case Right(files)              => Future.successful(files)
        }
      }

  }
}
