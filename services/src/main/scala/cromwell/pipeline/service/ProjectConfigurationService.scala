package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.ProjectConfigurationRepository
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.service.ProjectConfigurationService.Exceptions.{
  AccessDenied,
  InternalError,
  NotFound,
  ValidationError
}
import cromwell.pipeline.service.ProjectService.Exceptions.ProjectServiceException
import cromwell.pipeline.service.exceptions.ServiceException
import cromwell.pipeline.womtool.WomToolAPI

import java.nio.file.Path
import scala.concurrent.{ ExecutionContext, Future }

trait ProjectConfigurationService {

  def addConfiguration(projectConfiguration: ProjectConfiguration, userId: UserId): Future[Unit]

  def getLastByProjectId(projectId: ProjectId, userId: UserId): Future[ProjectConfiguration]

  def deactivateLastByProjectId(projectId: ProjectId, userId: UserId): Future[Unit]

  def buildConfiguration(
    projectId: ProjectId,
    projectFilePath: Path,
    version: Option[PipelineVersion],
    userId: UserId
  ): Future[ProjectConfiguration]

}

object ProjectConfigurationService {

  object Exceptions {
    sealed abstract class ProjectConfigurationServiceException(message: String) extends ServiceException(message)

    final case class NotFound(message: String = "Configuration not found")
        extends ProjectConfigurationServiceException(message)
    final case class AccessDenied(message: String = "Access denied. You are not the project owner")
        extends ProjectConfigurationServiceException(message)
    final case class InternalError(message: String = "Internal error")
        extends ProjectConfigurationServiceException(message)
    final case class ValidationError(message: String = "Unprocessable entity")
        extends ProjectConfigurationServiceException(message)
  }

  // scalastyle:off method.length
  def apply(
    repository: ProjectConfigurationRepository,
    projectService: ProjectService,
    womTool: WomToolAPI,
    projectVersioning: ProjectVersioning[VersioningException]
  )(
    implicit ec: ExecutionContext
  ): ProjectConfigurationService =
    new ProjectConfigurationService {

      def addConfiguration(projectConfiguration: ProjectConfiguration, userId: UserId): Future[Unit] =
        projectService
          .getUserProjectById(projectConfiguration.projectId, userId)
          .recoverWith { case e: ProjectServiceException => serviceErrorMapper(e) }
          .flatMap(
            _ =>
              repository.addConfiguration(projectConfiguration).recoverWith {
                case _ => internalError("add configuration")
              }
          )

      private def getByProjectId(projectId: ProjectId, userId: UserId): Future[Seq[ProjectConfiguration]] =
        projectService
          .getUserProjectById(projectId, userId)
          .recoverWith { case e: ProjectServiceException => serviceErrorMapper(e) }
          .flatMap(
            _ =>
              repository.getAllByProjectId(projectId).recoverWith {
                case _ => internalError("find configuration")
              }
          )

      private def getLastOptionByProjectId(projectId: ProjectId, userId: UserId): Future[Option[ProjectConfiguration]] =
        getByProjectId(projectId, userId).map(_.filter(_.active).sortBy(_.version).lastOption)

      def getLastByProjectId(projectId: ProjectId, userId: UserId): Future[ProjectConfiguration] =
        getLastOptionByProjectId(projectId, userId).flatMap {
          case Some(config) => Future.successful(config)
          case None         => notFoundProjectError(projectId)
        }

      def deactivateLastByProjectId(projectId: ProjectId, userId: UserId): Future[Unit] =
        getLastOptionByProjectId(projectId: ProjectId, userId: UserId).flatMap {
          case Some(config) => updateConfiguration(config.copy(active = false))
          case None         => notFoundProjectError(projectId)
        }

      private def updateConfiguration(projectConfiguration: ProjectConfiguration): Future[Unit] =
        repository.updateConfiguration(projectConfiguration).recoverWith {
          case _ => internalError("update project")
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
          getLastOptionByProjectId(projectId, userId).map {
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
                      WdlParams(file.path, nodes),
                      version
                    )
                )
              case Left(e) => Future.failed(ValidationError(e.toList.mkString(",")))
            }
          case Left(_) => internalError("get file")
        }
      }

      private def internalError(action: String): Future[Nothing] =
        Future.failed(InternalError(s"Failed to $action due to unexpected internal error"))

      private def notFoundProjectError(projectId: ProjectId) =
        Future.failed(NotFound(s"There is no configuration with project_id: ${projectId.value}"))

      private def serviceErrorMapper(exc: ProjectServiceException): Future[Nothing] =
        exc match {
          case _: ProjectService.Exceptions.AccessDenied  => Future.failed(AccessDenied())
          case _: ProjectService.Exceptions.NotFound      => Future.failed(NotFound())
          case e: ProjectService.Exceptions.InternalError => Future.failed(InternalError(e.getMessage))
        }
    }
  // scalastyle:on method.length
}
