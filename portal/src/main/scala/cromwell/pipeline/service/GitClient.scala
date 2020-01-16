package cromwell.pipeline.service

import java.io.File

import cromwell.pipeline.datastorage.dto.Project

trait GitClient {
  def updateFile(project: Project, file: File)
  def updateListOfFiles(project: Project, files: List[File])
  def createRepository(project: Project)
  def getListOfFiles(project: Project, file: File)
}
