package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cromwell.pipeline.controller.utils.FromStringUnmarshallers._
import cromwell.pipeline.controller.utils.PathMatchers.{ Path, ProjectId }
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.service.{ ProjectFileService, VersioningException }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

class ProjectFileController(wdlService: ProjectFileService)(implicit val executionContext: ExecutionContext) {

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
    entity(as[ProjectUpdateFileRequest]) { request =>
      onComplete(for {
        validateResponse <- wdlService.validateFile(request.projectFile.content)
        uploadResponse <- wdlService.uploadFile(projectId, request.projectFile, request.version, accessToken.userId)
      } yield {
        (validateResponse, uploadResponse) match {
          case (Right(_), Right(responseMessage)) => StatusCodes.OK.intValue -> responseMessage
          case (Left(_), Right(responseMessage))  => StatusCodes.Created.intValue -> responseMessage
          case (_, Left(response)) =>
            response match {
              case exception: VersioningException => StatusCodes.UnprocessableEntity.intValue -> exception.getMessage
            }
        }
      }) {
        case Success((status, p @ UpdateFiledResponse(_, _))) =>
          complete((status, p))
        case Success((status, message)) => complete((status, s"File have not uploaded due to $message"))
        case Failure(e)                 => complete(StatusCodes.InternalServerError, e.getMessage)
      }
    }
  }

  val route: AccessTokenContent => Route = implicit accessToken =>
    validateFile ~
      pathPrefix("projects" / ProjectId / "files") { projectId =>
        getFile(projectId) ~
        getFiles(projectId) ~
        uploadFile(projectId)
      }
}
