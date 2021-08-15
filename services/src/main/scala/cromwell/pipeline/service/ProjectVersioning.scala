package cromwell.pipeline.service

import java.nio.file.Path

import cromwell.pipeline.datastorage.dto._

import scala.concurrent.Future

trait ProjectVersioning[E <: VersioningException] {
  type AsyncResult[T] = Future[Either[E, T]]
  type ProjectFiles = List[ProjectFile]

  /**
   * Creates versioned repository for local project
   * @param localProject project that exists only locally and does not have real repository
   * @return full project with versioning-specific fields (`repositoryId`, `version`)
   */
  def createRepository(localProject: LocalProject): AsyncResult[Project]

  /**
   * Update file in repository with versioning
   * @param project project to which file belongs to
   * @param projectFile file to update
   * @param version preferable project version
   * @return new project version
   */
  def updateFile(
    project: Project,
    projectFile: ProjectFile,
    version: Option[PipelineVersion]
  ): AsyncResult[PipelineVersion]

  /**
   * Update files in repository with versioning
   * @param project project to which files belong to
   * @param projectFiles files to update
   * @param version preferable project version
   * @return new project version
   */
  def updateFiles(
    project: Project,
    projectFiles: ProjectFiles,
    version: Option[PipelineVersion]
  ): AsyncResult[PipelineVersion]

  /**
   * Retrieves project file from repository
   * @param project project to which file belongs to
   * @param path path to file
   * @param version preferable project version
   * @return file from repository
   */
  def getFile(project: Project, path: Path, version: Option[PipelineVersion]): AsyncResult[ProjectFile]

  /**
   * Retrieves all files from repository
   * @param project project to which files belongs to
   * @param version preferable project version
   * @return all project files from repository
   */
  def getFiles(project: Project, version: Option[PipelineVersion]): AsyncResult[ProjectFiles]

  /**
   * Returns all project versions that ever existed
   *
   * @param project project to fetch versions
   * @return all versions that project ever had
   */
  def getProjectVersions(project: Project): AsyncResult[List[PipelineVersion]]

  /**
   * Returns all file versions that ever existed.
   * Amount of file versions might be less then amount of project version because file could have been deleted
   * or added much later
   *
   * @param project project to fetch versions
   * @param path path to file
   * @return all versions that specific file ever had
   */
  def getFileVersions(project: Project, path: Path): AsyncResult[List[PipelineVersion]]

  /**
   * Returns all file commits
   *
   * @param project target project
   * @param path path to file
   * @return all commits of the specific file
   */
  def getFileCommits(project: Project, path: Path): AsyncResult[List[Commit]]

}
