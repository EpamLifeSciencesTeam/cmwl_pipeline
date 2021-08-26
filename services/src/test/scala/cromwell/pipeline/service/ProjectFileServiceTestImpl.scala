package cromwell.pipeline.service
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId

import java.nio.file.Path
import scala.concurrent.Future

class ProjectFileServiceTestImpl(projectFiles: Seq[ProjectFile], testMode: TestMode) extends ProjectFileService {

  override def validateFile(fileContent: ProjectFileContent): Future[Either[ValidationError, Unit]] =
    testMode match {
      case WithException(exc: ValidationError) => Future.successful(Left(exc))
      case WithException(exc)                  => Future.failed(exc)
      case _                                   => Future.successful(Right(()))
    }

  override def uploadFile(
    projectId: ProjectId,
    projectFile: ProjectFile,
    version: Option[PipelineVersion],
    userId: UserId
  ): Future[Either[VersioningException, Unit]] =
    testMode match {
      case WithException(exc: VersioningException) => Future.successful(Left(exc))
      case WithException(exc)                      => Future.failed(exc)
      case _                                       => Future.successful(Right(()))
    }

  override def getFile(
    projectId: ProjectId,
    path: Path,
    version: Option[PipelineVersion],
    userId: UserId
  ): Future[ProjectFile] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(projectFiles.find(_.path == path).get)
    }

  override def getFiles(
    projectId: ProjectId,
    version: Option[PipelineVersion],
    userId: UserId
  ): Future[List[ProjectFile]] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(projectFiles.toList)
    }

}

object ProjectFileServiceTestImpl {

  def apply(projectFiles: ProjectFile*): ProjectFileServiceTestImpl =
    new ProjectFileServiceTestImpl(projectFiles = projectFiles, testMode = Success)

  def withException(exception: Throwable): ProjectFileServiceTestImpl =
    new ProjectFileServiceTestImpl(projectFiles = Seq.empty, testMode = WithException(exception))

}
