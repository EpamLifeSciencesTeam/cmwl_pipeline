package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import cromwell.pipeline.controller.utils.FieldUnmarshallers._
import cromwell.pipeline.controller.utils.FromStringUnmarshallers._
import cromwell.pipeline.controller.utils.PathMatchers.{ Path, ProjectId => ProjectIdPM }
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.model.wrapper.ProjectId
import cromwell.pipeline.service.ProjectFileService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import java.nio.file._
import scala.concurrent.ExecutionContext
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

  private def deleteFile(projectId: ProjectId)(implicit accessToken: AccessTokenContent): Route = delete {
    path(Path) { projectFilePath =>
      parameter('version.as[PipelineVersion].optional) { version =>
        onComplete(wdlService.deleteFile(projectId, projectFilePath, version, accessToken.userId)) {
          case Success(_) => complete(StatusCodes.OK)
          case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
        }
      }
    }
  }

  val route: AccessTokenContent => Route = implicit accessToken =>
    validateFile ~
      pathPrefix("projects" / ProjectIdPM / "files") { projectId =>
        getFile(projectId) ~
        getFiles(projectId) ~
        uploadFile(projectId) ~
        deleteFile(projectId)
      }
}
