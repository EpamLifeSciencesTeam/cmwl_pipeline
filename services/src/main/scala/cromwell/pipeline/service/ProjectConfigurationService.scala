package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.ProjectConfigurationRepository
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.womtool.WomToolAPI

import java.nio.file.Path
import scala.concurrent.{ ExecutionContext, Future }

trait ProjectConfigurationService {

  def addConfiguration(projectConfiguration: ProjectConfiguration, userId: UserId): Future[Unit]

  def getLastByProjectId(projectId: ProjectId, userId: UserId): Future[Option[ProjectConfiguration]]

  def deactivateLastByProjectId(projectId: ProjectId, userId: UserId): Future[Unit]

  def buildConfiguration(
    projectId: ProjectId,
    projectFilePath: Path,
    version: Option[PipelineVersion],
    userId: UserId
  ): Future[ProjectConfiguration]

}

object ProjectConfigurationService {

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
          .flatMap(_ => repository.addConfiguration(projectConfiguration))

      private def getByProjectId(projectId: ProjectId, userId: UserId): Future[Seq[ProjectConfiguration]] =
        projectService.getUserProjectById(projectId, userId).flatMap(_ => repository.getAllByProjectId(projectId))

      def getLastByProjectId(projectId: ProjectId, userId: UserId): Future[Option[ProjectConfiguration]] =
        getByProjectId(projectId, userId).map(
          _.filter(_.active).sortBy(_.version).lastOption
        )

      def deactivateLastByProjectId(projectId: ProjectId, userId: UserId): Future[Unit] =
        getLastByProjectId(projectId: ProjectId, userId: UserId).flatMap {
          case Some(config) => updateConfiguration(config.copy(active = false))
          case _            => Future.failed(new RuntimeException("There is no project to deactivate"))
        }

      private def updateConfiguration(projectConfiguration: ProjectConfiguration): Future[Unit] =
        repository.updateConfiguration(projectConfiguration)

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
          getLastByProjectId(projectId, userId).map {
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
              case Left(e) => Future.failed(ValidationError(e.toList))
            }
          case Left(versioningException) => Future.failed(versioningException)
        }
      }
    }

}
