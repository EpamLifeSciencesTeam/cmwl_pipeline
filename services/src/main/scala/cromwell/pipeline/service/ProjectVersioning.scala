package cromwell.pipeline.service

import java.nio.file.Path

import cromwell.pipeline.datastorage.dto.{ Project, ProjectFile, Version }

import scala.concurrent.{ ExecutionContext, Future }

trait ProjectVersioning[E >: VersioningException] {
  type AsyncResult[T] = Future[Either[E, T]]
  type ProjectFiles = List[ProjectFile]

  def updateFile(project: Project, projectFile: ProjectFile, version: Option[Version])(
    implicit ec: ExecutionContext
  ): AsyncResult[String]

  def updateFiles(project: Project, projectFiles: ProjectFiles)(
    implicit ec: ExecutionContext
  ): AsyncResult[List[String]]

  def createRepository(project: Project)(
    implicit ec: ExecutionContext
  ): AsyncResult[Project]

  def getFiles(project: Project, path: Path)(
    implicit ec: ExecutionContext
  ): AsyncResult[List[String]]

  def getProjectVersions(project: Project)(
    implicit ec: ExecutionContext
  ): AsyncResult[Seq[Version]]

  def getFileVersions(project: Project, path: Path)(
    implicit ec: ExecutionContext
  ): AsyncResult[List[Version]]

  def getFilesVersions(project: Project, path: Path)(
    implicit ec: ExecutionContext
  ): AsyncResult[List[Version]]

  def getFileTree(project: Project, version: Option[Version] = None)(
    implicit ec: ExecutionContext
  ): AsyncResult[List[String]]

  def getFile(project: Project, path: Path, version: Option[Version] = None)(
    implicit ec: ExecutionContext
  ): AsyncResult[ProjectFile]
}

case class VersioningException(message: String) extends Exception(message)
