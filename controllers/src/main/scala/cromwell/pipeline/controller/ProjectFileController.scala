package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import cromwell.pipeline.controller.utils.FieldUnmarshallers._
import cromwell.pipeline.controller.utils.FromStringUnmarshallers._
import cromwell.pipeline.controller.utils.PathMatchers.{ Path, ProjectId }
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.service.{ ProjectFileService, VersioningException }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import java.nio.file._
import scala.collection.immutable
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

class ProjectFileController(wdlService: ProjectFileService)(
  implicit val executionContext: ExecutionContext,
  val materializer: Materializer
) {

  private val validateFile: Route = post {
    pathPrefix("files" / "validation") {
      entity(as[ValidateFileContentRequest]) { request =>
        onComplete(wdlService.validateFile(request.content)) {
          case Success(Left(e)) => complete(StatusCodes.Conflict, e.errors)
          case Success(_)       => complete(StatusCodes.OK)
          case Failure(e)       => complete(StatusCodes.InternalServerError, e.getMessage)
        }
      }
    }
  }

  private def getFile(projectId: ProjectId)(implicit accessToken: AccessTokenContent): Route = get {
    path(Path) { projectFilePath =>
      parameter('version.as[PipelineVersion].optional) { version =>
        onComplete(wdlService.getFile(projectId, projectFilePath, version, accessToken.userId)) {
          case Success(projectFile) => complete(projectFile)
          case Failure(e)           => complete(StatusCodes.NotFound, e.getMessage)
        }
      }
    }
  }

  private def getFiles(projectId: ProjectId)(implicit accessToken: AccessTokenContent): Route = get {
    parameter('version.as[PipelineVersion].optional) { version =>
      onComplete(wdlService.getFiles(projectId, version, accessToken.userId)) {
        case Success(projectFile) => complete(projectFile)
        case Failure(e)           => complete(StatusCodes.NotFound, e.getMessage)
      }
    }
  }

  private def uploadFile(projectId: ProjectId)(implicit accessToken: AccessTokenContent): Route = post {
    formFields('path.as[Path], 'version.as[PipelineVersion].optional) { (path, version) =>
      fileUpload("file") {
        case (_, byteSource) =>
          onSuccess(byteSource.map(_.utf8String).runFold("")(_ + _)) { content =>
            val uploadedFile: ProjectFile =
              ProjectFile(path, ProjectFileContent(content))
            onComplete {
              for {
                validateResponse <- wdlService.validateFile(uploadedFile.content)
                uploadResponse <- wdlService.uploadFile(
                  projectId,
                  uploadedFile,
                  version,
                  accessToken.userId
                )
              } yield (validateResponse, uploadResponse) match {
                case (Right(_), Right(_)) => Right(StatusCodes.OK)
                case (Left(_), Right(_))  => Right(StatusCodes.Created)
                case (_, Left(response))  => Left(response.getMessage)
              }
            } {
              case Success(Right(sc)) => complete(sc)
              case Success(Left(error)) =>
                complete(StatusCodes.UnprocessableEntity, s"File have not uploaded due to $error")
              case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
            }
          }
      }
    }
  }

  private def uploadAllFiles(projectId: ProjectId)(implicit accessToken: AccessTokenContent): Route = post {
    case class FilePathAndResponse(
                                    path: Path,
                                    validateResponse: Either[ValidationError, Unit],
                                    uploadResponse: Either[VersioningException, Unit]
                                  )

    formField('version.as[PipelineVersion].optional) { version =>
      fileUploadAll("file") { byteSources =>
        formField('path.as[Path]) { path =>
          byteSources.map { byteSource =>
            val contentF: Future[String] = byteSource._2.map(_.utf8String).runFold("")(_ + _)
            val uploadedFileF: Future[ProjectFile] = contentF.map(x => ProjectFile(path, ProjectFileContent(x)))

            onComplete {
              uploadedFileF.map { uploadedFile =>
                for {
                  validateResponse <- wdlService.validateFile(uploadedFile.content)
                  uploadResponse <- wdlService.uploadFile(
                    projectId,
                    uploadedFile,
                    version,
                    accessToken.userId
                  )
                } yield (validateResponse, uploadResponse) match {
                  case (Right(_), Right(_)) => Right(StatusCodes.OK)
                  case (Left(_), Right(_)) => Right(StatusCodes.Created)
                  case (_, Left(response)) => Left(response.getMessage)
                }
              }
            }
          } {
            case _ => complete(StatusCodes.OK)
          }
        }
      }
    }
  }

//  private def uploadAllFiles(projectId: ProjectId)(implicit accessToken: AccessTokenContent): Route = post {
//
//    case class FilePathAndResponse(
//      path: Path,
//      validateResponse: Either[ValidationError, Unit],
//      uploadResponse: Either[VersioningException, Unit])
//
//    formField('version.as[PipelineVersion].optional) { version =>
//      fileUploadAll("file") { byteSources =>
//        formField('path.as[Path]) { path =>
//          byteSources.map { byteSource =>
//
//            val contentF: Future[String] = byteSource._2.map(_.utf8String).runFold("")(_ + _)
//            val uploadedFileF: Future[ProjectFile] = contentF.map(x => ProjectFile(path, ProjectFileContent(x)))
//
//            onComplete {
//              uploadedFileF.map { uploadedFile =>
//              for {
//                  validateResponse <- wdlService.validateFile(uploadedFile.content)
//                  uploadResponse <- wdlService.uploadFile(
//                    projectId,
//                    uploadedFile,
//                    version,
//                    accessToken.userId
//                  )
//                } yield (validateResponse, uploadResponse) match {
//                  case (Right(_), Right(_)) => Right(StatusCodes.OK)
//                  case (Left(_), Right(_)) => Right(StatusCodes.Created)
//                  case (_, Left(response)) => Left(response.getMessage)
//                }
//              }
//            }
//                {
//                  case Success(Right(sc)) => complete(sc)
//                  case Success(Left(error)) =>
//                    complete(StatusCodes.UnprocessableEntity, s"File have not uploaded due to $error")
//                  case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
//                }
//              }
//            }
//          }.map { x =>
//
//          }
//        }
//      }

  val route: AccessTokenContent => Route = implicit accessToken =>
    validateFile ~
      pathPrefix("projects" / ProjectId / "files") { projectId =>
        getFile(projectId) ~
        getFiles(projectId) ~
        uploadFile(projectId) ~
        uploadAllFiles(projectId)
      }
}
