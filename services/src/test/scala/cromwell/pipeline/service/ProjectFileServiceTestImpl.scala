package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId

import java.nio.file.Path
import scala.concurrent.Future

class ProjectFileServiceTestImpl(valToReturn: DummyToReturn) extends ProjectFileService {

  lazy val wrongReturnedTypeException = new Exception("Wrong returned test value type")

  override def validateFile(fileContent: ProjectFileContent): Future[Either[ValidationError, Unit]] =
    valToReturn match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(Right(Unit))
    }

  override def uploadFile(
    projectId: ProjectId,
    projectFile: ProjectFile,
    version: Option[PipelineVersion],
    userId: UserId
  ): Future[Either[VersioningException, UpdateFiledResponse]] =
    valToReturn match {
      case WithException(exc)                    => Future.failed(exc)
      case UpdateFiledResponseToReturn(response) => Future.successful(Right(response))
      case _                                     => throw wrongReturnedTypeException
    }

  override def getFile(
    projectId: ProjectId,
    path: Path,
    version: Option[PipelineVersion],
    userId: UserId
  ): Future[ProjectFile] =
    valToReturn match {
      case WithException(exc)        => Future.failed(exc)
      case ProjectFileToReturn(file) => Future.successful(file)
      case _                         => throw wrongReturnedTypeException
    }

  override def getFiles(
    projectId: ProjectId,
    version: Option[PipelineVersion],
    userId: UserId
  ): Future[List[ProjectFile]] =
    valToReturn match {
      case WithException(exc)          => Future.failed(exc)
      case ProjectFilesToReturn(files) => Future.successful(files)
      case _                           => throw wrongReturnedTypeException
    }

  override def buildConfiguration(
    projectId: ProjectId,
    projectFilePath: Path,
    version: Option[PipelineVersion],
    userId: UserId
  ): Future[ProjectConfiguration] =
    valToReturn match {
      case WithException(exc)                          => Future.failed(exc)
      case ProjectConfigurationToReturn(configuration) => Future.successful(configuration)
      case _                                           => throw wrongReturnedTypeException
    }

}

object ProjectFileServiceTestImpl {
  def apply(valToReturn: DummyToReturn): ProjectFileServiceTestImpl =
    new ProjectFileServiceTestImpl(valToReturn)

}
