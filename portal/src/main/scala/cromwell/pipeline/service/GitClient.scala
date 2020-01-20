package cromwell.pipeline.service

import java.io.File

import cromwell.pipeline.datastorage.dto.Project

import scala.concurrent.Future

trait GitClient {

  def updateFile(project: Project, file: File): Future[Option[File]]
  def updateListOfFiles(project: Project, files: List[File]): Future[Option[List[File]]]
  def createRepository(project: Project): Future[Option[Project]]
  def getListOfFiles(project: Project, file: List[File]): Future[Option[List[File]]]
}
