package cromwell.pipeline.service.impls

import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.service.{ ProjectFileService, VersioningException }

import java.nio.file.Path
import scala.concurrent.Future

class ProjectFileServiceTestImpl(projectFileBundles: Map[ProjectId, Seq[ProjectFile]], testMode: TestMode)
    extends ProjectFileService {

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
      case _ =>
        projectFileBundles.get(projectId).flatMap(seq => seq.find(_.path == path)) match {
          case Some(value) => Future.successful(value)
          case None        => Future.failed(new RuntimeException("No correct test file bundle received"))
        }
    }

  override def getFiles(
    projectId: ProjectId,
    version: Option[PipelineVersion],
    userId: UserId
  ): Future[List[ProjectFile]] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(projectFileBundles.getOrElse(projectId, List.empty).toList)
    }

  override def deleteFile(
    projectId: ProjectId,
    path: Path,
    version: Option[PipelineVersion],
    userId: UserId
  ): Future[Either[VersioningException, Unit]] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(Right(()))
    }
}

object ProjectFileServiceTestImpl {

  def apply(projectFileBundles: (ProjectId, Seq[ProjectFile])*): ProjectFileServiceTestImpl =
    new ProjectFileServiceTestImpl(projectFileBundles = projectFileBundles.toMap, testMode = Success)

  def withException(exception: Throwable): ProjectFileServiceTestImpl =
    new ProjectFileServiceTestImpl(projectFileBundles = Map.empty, testMode = WithException(exception))

}
