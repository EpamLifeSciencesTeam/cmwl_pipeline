package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dto._

import java.nio.file.Path
import scala.concurrent.{ ExecutionContext, Future }

class ProjectVersioningTestImpl(valToReturn: DummyToReturn) extends ProjectVersioning[VersioningException] {

  lazy val wrongReturnedTypeException = new Exception("Wrong returned test value type")

  override def updateFile(project: Project, projectFile: ProjectFile, version: PipelineVersion)(
    implicit ec: ExecutionContext
  ): AsyncResult[UpdateFiledResponse] =
    valToReturn match {
      case WithException(exc)                    => Future.failed(exc)
      case UpdateFiledResponseToReturn(response) => Future.successful(Right(response))
      case _                                     => throw wrongReturnedTypeException
    }

  override def updateFiles(project: Project, projectFiles: ProjectFiles)(
    implicit ec: ExecutionContext
  ): AsyncResult[List[SuccessResponseMessage]] =
    valToReturn match {
      case WithException(exc)                                => Future.failed(exc)
      case SuccessResponseMessagesToReturn(responseMessages) => Future.successful(Right(responseMessages))
      case _                                                 => throw wrongReturnedTypeException
    }

  override def createRepository(localProject: LocalProject)(implicit ec: ExecutionContext): AsyncResult[Project] =
    valToReturn match {
      case WithException(exc)       => Future.failed(exc)
      case ProjectToReturn(project) => Future.successful(Right(project))
      case _                        => throw wrongReturnedTypeException
    }

  override def getFiles(project: Project, version: Option[PipelineVersion])(
    implicit ec: ExecutionContext
  ): AsyncResult[List[ProjectFile]] =
    valToReturn match {
      case WithException(exc)          => Future.failed(exc)
      case ProjectFilesToReturn(files) => Future.successful(Right(files))
      case _                           => throw wrongReturnedTypeException
    }

  override def getProjectVersions(project: Project)(implicit ec: ExecutionContext): AsyncResult[Seq[GitLabVersion]] =
    valToReturn match {
      case WithException(exc)               => Future.failed(exc)
      case GitLabVersionsToReturn(versions) => Future.successful(Right(versions))
      case _                                => throw wrongReturnedTypeException
    }

  override def getFileCommits(project: Project, path: Path)(
    implicit ec: ExecutionContext
  ): AsyncResult[Seq[FileCommit]] =
    valToReturn match {
      case WithException(exc)           => Future.failed(exc)
      case FileCommitsToReturn(commits) => Future.successful(Right(commits))
      case _                            => throw wrongReturnedTypeException
    }

  override def getFileVersions(project: Project, path: Path)(
    implicit ec: ExecutionContext
  ): AsyncResult[Seq[GitLabVersion]] =
    valToReturn match {
      case WithException(exc)               => Future.failed(exc)
      case GitLabVersionsToReturn(versions) => Future.successful(Right(versions))
      case _                                => throw wrongReturnedTypeException
    }

  override def getFilesVersions(project: Project, path: Path)(
    implicit ec: ExecutionContext
  ): AsyncResult[List[GitLabVersion]] =
    getProjectVersions(project).flatMap {
      case Right(value) => Future.successful(Right(value.toList))
      case Left(exc)    => Future.failed(exc)
    }

  override def getFilesTree(project: Project, version: Option[PipelineVersion])(
    implicit ec: ExecutionContext
  ): AsyncResult[Seq[FileTree]] =
    valToReturn match {
      case WithException(exc)           => Future.failed(exc)
      case FileTreesToReturn(fileTrees) => Future.successful(Right(fileTrees))
      case _                            => throw wrongReturnedTypeException
    }

  override def getFile(project: Project, path: Path, version: Option[PipelineVersion])(
    implicit ec: ExecutionContext
  ): AsyncResult[ProjectFile] =
    valToReturn match {
      case WithException(exc)        => Future.failed(exc)
      case ProjectFileToReturn(file) => Future.successful(Right(file))
      case _                         => throw wrongReturnedTypeException
    }

  override def getDefaultProjectVersion()(implicit ec: ExecutionContext): PipelineVersion =
    valToReturn match {
      case PipelineVersionToReturn(version) => version
      case _                                => throw wrongReturnedTypeException
    }

  override def getUpdatedProjectVersion(project: Project, optionUserVersion: Option[PipelineVersion])(
    implicit ec: ExecutionContext
  ): AsyncResult[PipelineVersion] =
    valToReturn match {
      case WithException(exc)               => Future.failed(exc)
      case PipelineVersionToReturn(version) => Future.successful(Right(version))
      case _                                => throw wrongReturnedTypeException
    }
}

object ProjectVersioningTestImpl {
  def apply(valToReturn: DummyToReturn): ProjectVersioningTestImpl =
    new ProjectVersioningTestImpl(valToReturn)

}
