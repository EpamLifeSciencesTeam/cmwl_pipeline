package cromwell.pipeline.service

import cats.data.EitherT
import cats.implicits._
import cromwell.pipeline.datastorage.dto.{ CromwellInput, PipelineVersion, Project, ProjectFile, ProjectId, Run }
import cromwell.pipeline.model.wrapper.UserId

import java.nio.file.{ Path, Paths }
import scala.concurrent.{ ExecutionContext, Future }

class AggregationService(
  projectService: ProjectService,
  projectVersioning: ProjectVersioning[VersioningException],
  projectConfigurationService: ProjectConfigurationService
)(implicit executionContext: ExecutionContext) {

  def aggregate(run: Run): Future[CromwellInput] = {
    val version = PipelineVersion(run.projectVersion)
    val projectFiles =
      getFilesByProjectId(run.projectId, run.userId, Some(version)).valueOrF(e => Future.failed(e))
    val futureFileConfigurations =
      projectConfigurationService.getLastByProjectId(run.projectId, run.userId).flatMap {
        case Some(config) => Future.successful(config.projectFileConfigurations)
        case None         => Future.failed(new RuntimeException("Configurations for projectId " + run.projectId + " not found"))
      }
    for {
      files <- projectFiles
      configs <- futureFileConfigurations
    } yield CromwellInput(run.projectId, run.userId, version, files, configs)
  }

  private def getFilesByProjectId(
    projectId: ProjectId,
    userId: UserId,
    version: Option[PipelineVersion]
  ): EitherT[Future, VersioningException, List[ProjectFile]] = {

    type EitherF[T] = EitherT[Future, VersioningException, T]

    def getProjectFile(
      project: Project,
      path: Path,
      version: Option[PipelineVersion]
    ): EitherF[ProjectFile] =
      EitherT(projectVersioning.getFile(project, path, version))

    for {
      project <- EitherT.right(projectService.getUserProjectById(projectId, userId))
      trees <- EitherT(projectVersioning.getFilesTree(project, version))
      files <- trees.toList.traverse(tree => getProjectFile(project, Paths.get(tree.path), version))
    } yield files
  }
}
