package cromwell.pipeline.service

import cats.data.EitherT
import cats.implicits._
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import scala.concurrent.{ ExecutionContext, Future }

trait AggregationService {

  def aggregate(run: Run): Future[CromwellInput]

}

object AggregationService {

  def apply(
    projectService: ProjectService,
    projectVersioning: ProjectVersioning[VersioningException],
    projectConfigurationService: ProjectConfigurationService
  )(implicit executionContext: ExecutionContext): AggregationService =
    new AggregationService {

      def aggregate(run: Run): Future[CromwellInput] = {
        val version = PipelineVersion(run.projectVersion)
        val projectFiles =
          getFilesByProjectId(run.projectId, run.userId, Some(version)).valueOrF(e => Future.failed(e))
        val futureWdlParams = projectConfigurationService.getLastByProjectId(run.projectId, run.userId).map(_.wdlParams)

        for {
          files <- projectFiles
          wdlParams <- futureWdlParams
        } yield CromwellInput(run.projectId, run.userId, version, files, wdlParams)
      }

      private def getFilesByProjectId(
        projectId: ProjectId,
        userId: UserId,
        version: Option[PipelineVersion]
      ): EitherT[Future, VersioningException, List[ProjectFile]] =
        for {
          project <- EitherT.right(projectService.getUserProjectById(projectId, userId))
          files <- EitherT(projectVersioning.getFiles(project, version))
        } yield files
    }

}
