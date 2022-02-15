package cromwell.pipeline.service.impls

import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.validator.Enable
import cromwell.pipeline.model.wrapper.{ ProjectConfigurationId, ProjectId, UserId }
import cromwell.pipeline.service.ProjectConfigurationService

import java.nio.file.Path
import java.util.UUID
import scala.concurrent.Future

class ProjectConfigurationServiceTestImpl(projectConfigurations: Seq[ProjectConfiguration], testMode: TestMode)
    extends ProjectConfigurationService {

  override def addConfiguration(projectConfiguration: ProjectConfiguration, userId: UserId): Future[Unit] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.unit
    }

  override def getLastByProjectId(projectId: ProjectId, userId: UserId): Future[ProjectConfiguration] =
    testMode match {
      case WithException(exc) => Future.failed(exc)

      case _ =>
        projectConfigurations.find(conf => conf.projectId == projectId) match {
          case Some(configuration) => Future.successful(configuration)
          case _                   => Future.failed(ProjectConfigurationService.Exceptions.NotFound())
        }
    }

  override def deactivateLastByProjectId(projectId: ProjectId, userId: UserId): Future[Unit] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.unit
    }

  override def buildConfiguration(
    projectId: ProjectId,
    projectFilePath: Path,
    version: Option[PipelineVersion],
    userId: UserId
  ): Future[ProjectConfiguration] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _ =>
        val config = ProjectConfiguration(
          id = ProjectConfigurationId(UUID.randomUUID().toString, Enable.Unsafe),
          projectId = projectId,
          active = true,
          wdlParams = WdlParams(projectFilePath, Nil),
          version = ProjectConfigurationVersion.defaultVersion
        )
        Future.successful(config)
    }

}

object ProjectConfigurationServiceTestImpl {

  def apply(projectConfigurations: ProjectConfiguration*): ProjectConfigurationServiceTestImpl =
    new ProjectConfigurationServiceTestImpl(projectConfigurations = projectConfigurations, testMode = Success)

  def withException(exception: Throwable): ProjectConfigurationServiceTestImpl =
    new ProjectConfigurationServiceTestImpl(projectConfigurations = Seq.empty, testMode = WithException(exception))

}
