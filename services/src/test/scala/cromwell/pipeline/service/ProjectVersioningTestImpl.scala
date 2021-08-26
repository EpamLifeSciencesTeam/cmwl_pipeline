package cromwell.pipeline.service
import cromwell.pipeline.datastorage.dto._

import java.nio.file.Path
import scala.concurrent.Future

class ProjectVersioningTestImpl(
  projects: List[Project],
  pipelineVersions: List[PipelineVersion],
  projectFiles: List[ProjectFile],
  commits: List[Commit],
  testMode: TestMode
) extends ProjectVersioning[VersioningException] {

  override def createRepository(localProject: LocalProject): AsyncResult[Project] =
    returnValOrExc(projects.head)

  override def updateFile(
    project: Project,
    projectFile: ProjectFile,
    version: Option[PipelineVersion]
  ): AsyncResult[PipelineVersion] =
    returnValOrExc(pipelineVersions.head)

  override def updateFiles(
    project: Project,
    projectFiles: ProjectFiles,
    version: Option[PipelineVersion]
  ): AsyncResult[PipelineVersion] =
    returnValOrExc(pipelineVersions.head)

  override def getFile(project: Project, path: Path, version: Option[PipelineVersion]): AsyncResult[ProjectFile] =
    returnValOrExc(projectFiles.find(_.path == path).get)

  override def getFiles(project: Project, version: Option[PipelineVersion]): AsyncResult[ProjectFiles] =
    returnValOrExc(projectFiles)

  override def getProjectVersions(project: Project): AsyncResult[List[PipelineVersion]] =
    returnValOrExc(pipelineVersions)

  override def getFileVersions(project: Project, path: Path): AsyncResult[List[PipelineVersion]] =
    returnValOrExc(pipelineVersions)

  override def getFileCommits(project: Project, path: Path): AsyncResult[List[Commit]] =
    returnValOrExc(commits)

  private def returnValOrExc[T](valToReturn: => T): AsyncResult[T] =
    testMode match {
      case WithException(exc: VersioningException) => Future.successful(Left(exc))
      case WithException(exc)                      => Future.failed(exc)
      case _                                       => Future.successful(Right(valToReturn))
    }

}

object ProjectVersioningTestImpl {

  def apply(
    projects: List[Project] = Nil,
    pipelineVersions: List[PipelineVersion] = Nil,
    projectFiles: List[ProjectFile] = Nil,
    commits: List[Commit] = Nil
  ): ProjectVersioningTestImpl =
    new ProjectVersioningTestImpl(projects, pipelineVersions, projectFiles, commits, testMode = Success)

  def withException(exception: Throwable): ProjectVersioningTestImpl =
    new ProjectVersioningTestImpl(
      projects = Nil,
      pipelineVersions = Nil,
      projectFiles = Nil,
      commits = Nil,
      testMode = WithException(exception)
    )

}
