package cromwell.pipeline.service

import java.nio.file.Path

import cromwell.pipeline.datastorage.dto.{Project, ProjectFile, Version}

import scala.concurrent.Future

trait ProjectVersioning[E >: VersioningException] {
  type AsyncResult[T] = Future[Either[E, T]]
  type ProjectFiles = List[ProjectFile]

  def updateFile(project: Project, projectFile: ProjectFile): AsyncResult[String]
  def updateFiles(project: Project, projectFiles: ProjectFiles): AsyncResult[List[String]]
  def createRepository(project: Project): AsyncResult[Project]
  def getFiles(project: Project, path: Path): AsyncResult[List[String]]
  def getProjectVersions(project: Project): AsyncResult[Project]
  def getFileVersions(project: Project, path: Path): AsyncResult[List[Version]]
  def getFilesVersions(project: Project, path: Path): AsyncResult[List[Version]]
  def getFileTree(project: Project, version: Option[Version] = None): AsyncResult[List[String]]
  def getFile(project: Project, path: Path, version: Option[Version] = None): AsyncResult[String]
}

case class VersioningException(message: String) extends Exception(message)
