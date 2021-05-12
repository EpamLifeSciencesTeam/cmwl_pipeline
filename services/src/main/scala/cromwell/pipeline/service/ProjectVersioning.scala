package cromwell.pipeline.service

import java.nio.file.Path

import cromwell.pipeline.datastorage.dto._

import scala.concurrent.{ ExecutionContext, Future }

trait ProjectVersioning[E >: VersioningException] {
  type AsyncResult[T] = Future[Either[E, T]]
  type ProjectFiles = List[ProjectFile]

  def updateFile(project: Project, projectFile: ProjectFile, version: PipelineVersion)(
    implicit ec: ExecutionContext
  ): AsyncResult[UpdateFiledResponse]

  def updateFiles(project: Project, projectFiles: ProjectFiles)(
    implicit ec: ExecutionContext
  ): AsyncResult[List[SuccessResponseMessage]]

  def createRepository(localProject: LocalProject)(
    implicit ec: ExecutionContext
  ): AsyncResult[Project]

  def getFiles(project: Project, path: Path)(
    implicit ec: ExecutionContext
  ): AsyncResult[List[String]]

  def getProjectVersions(project: Project)(
    implicit ec: ExecutionContext
  ): AsyncResult[Seq[GitLabVersion]]

  def getFileCommits(project: Project, path: Path)(
    implicit ec: ExecutionContext
  ): AsyncResult[Seq[FileCommit]]

  def getFileVersions(project: Project, path: Path)(
    implicit ec: ExecutionContext
  ): AsyncResult[Seq[GitLabVersion]]

  def getFilesVersions(project: Project, path: Path)(
    implicit ec: ExecutionContext
  ): AsyncResult[List[GitLabVersion]]

  def getFilesTree(project: Project, version: Option[PipelineVersion] = None)(
    implicit ec: ExecutionContext
  ): AsyncResult[Seq[FileTree]]

  def getFile(project: Project, path: Path, version: Option[PipelineVersion] = None)(
    implicit ec: ExecutionContext
  ): AsyncResult[ProjectFile]

  def getDefaultProjectVersion()(
    implicit ec: ExecutionContext
  ): PipelineVersion

  def getUpdatedProjectVersion(project: Project, optionUserVersion: Option[PipelineVersion])(
    implicit ec: ExecutionContext
  ): AsyncResult[PipelineVersion]
}
