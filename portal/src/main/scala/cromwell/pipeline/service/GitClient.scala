package cromwell.pipeline.service

import java.nio.file.Path

import cromwell.pipeline.datastorage.dto.{ Project, Version }

import scala.concurrent.Future

trait ProjectVersioning[E >: VersioningException] {

  type AsyncResult[T] = Future[Either[E, T]]

  def updateFile(project: Project, path: Path, content: String): AsyncResult[String]
  def updateListOfFiles(project: Project, path: Path, content: List[String]): AsyncResult[List[String]]
  def createRepository(project: Project, path: Path): AsyncResult[Project]
  def getListOfFiles(project: Project, path: Path): AsyncResult[List[String]]
  def getVersionsOfProject(project: Project): AsyncResult[Project]
  def getVersionsOfFiles(project: Project, path: Path): AsyncResult[List[Version]]

  def getFileTree(project: Project, version: Option[Version] = None): AsyncResult[List[String]]

  def getFile(project: Project, path: Path, version: Option[Version] = None): AsyncResult[String]
}

case class VersioningException(message: String) extends Exception(message)
