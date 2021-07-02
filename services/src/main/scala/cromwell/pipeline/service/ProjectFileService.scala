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
  ): Future[Either[VersioningException, UpdateFiledResponse]]

  def getFile(
    projectId: ProjectId,
    path: Path,
    version: Option[PipelineVersion] = None,
    userId: UserId
  ): Future[ProjectFile]

  def buildConfiguration(
    projectId: ProjectId,
    projectFilePath: Path,
    version: Option[PipelineVersion],
    userId: UserId
  ): Future[ProjectConfiguration]

}

object ProjectFileService {

  def apply(
    projectService: ProjectService,
    projectConfigurationService: ProjectConfigurationService,
    womTool: WomToolAPI,
    projectVersioning: ProjectVersioning[VersioningException]
  )(
    implicit executionContext: ExecutionContext
  ): ProjectFileService =
    new ProjectFileService {

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
        projectService.getUserProjectById(projectId, userId).flatMap { project =>
          projectVersioning.getUpdatedProjectVersion(project, version).flatMap {
            case Left(versioningException) => Future.successful(Left(versioningException))
            case Right(newVersion) =>
              projectVersioning.updateFile(project, projectFile, newVersion).flatMap {
                case Left(versioningException) => Future.successful(Left(versioningException))
                case Right(response) =>
                  projectService.updateProjectVersion(projectId, newVersion, userId).map(_ => Right(response))
              }
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
            case Left(versioningException) => Future.failed(versioningException)
            case Right(projectFile)        => Future.successful(projectFile)
          }
        }

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

        val configurationVersion =
          projectConfigurationService.getLastByProjectId(projectId, userId).map {
            case Some(configuration) => configuration.version.increaseValue
            case None                => ProjectConfigurationVersion.defaultVersion
          }

        eitherFile.flatMap {
          case Right(file) =>
            womTool.inputsToList(file.content.content) match {
              case Right(nodes) =>
                configurationVersion.map(
                  version =>
                    ProjectConfiguration(
                      ProjectConfigurationId.randomId,
                      projectId,
                      active = true,
                      List(ProjectFileConfiguration(file.path, nodes)),
                      version
                    )
                )
              case Left(e) => Future.failed(ValidationError(e.toList))
            }
          case Left(versioningException) => Future.failed(versioningException)
        }
      }

    }

}
