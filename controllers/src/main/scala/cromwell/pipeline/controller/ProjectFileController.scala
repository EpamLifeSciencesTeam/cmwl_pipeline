package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.datastorage.dto.{
  PipelineVersion,
  ProjectId,
  ProjectUpdateFileRequest,
  UpdateFiledResponse,
  ValidateFileContentRequest,
  ValidationError
}
import cromwell.pipeline.service.{ ProjectFileService, VersioningException }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import cromwell.pipeline.controller.utils.FromStringUnmarshallers._

import java.nio.file.Path
import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

class ProjectFileController(wdlService: ProjectFileService)(implicit val executionContext: ExecutionContext) {

  private val validateFile: Route = path("validation") {
    post {
      entity(as[ValidateFileContentRequest]) { request =>
        onComplete(wdlService.validateFile(request.content)) {
          case Success(Left(e)) => complete(StatusCodes.Conflict, e.errors)
          case Success(_)       => complete(StatusCodes.OK)
          case Failure(e)       => complete(StatusCodes.InternalServerError, e.getMessage)
        }
      }
    }
  }

  private def uploadFile(implicit accessToken: AccessTokenContent): Route = post {
    entity(as[ProjectUpdateFileRequest]) { request =>
      onComplete(for {
        validateResponse <- wdlService.validateFile(request.projectFile.content)
        uploadResponse <- wdlService
          .uploadFile(request.projectId, request.projectFile, request.version, accessToken.userId)
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

  private def buildConfiguration(implicit accessToken: AccessTokenContent): Route =
    path("configurations") {
      get {
        parameters('project_id.as[ProjectId], 'project_file_path.as[Path], 'version.as[PipelineVersion].optional) {
          (projectId, projectFilePath, version) =>
            onComplete(
              wdlService.buildConfiguration(
                projectId,
                projectFilePath,
                version,
                accessToken.userId
              )
            ) {
              case Success(configuration)        => complete(configuration)
              case Failure(ValidationError(msg)) => complete(StatusCodes.UnprocessableEntity, msg)
              case Failure(e)                    => complete(StatusCodes.InternalServerError, e.getMessage)
            }
        }
      }
    }

  val route: AccessTokenContent => Route = implicit accessToken =>
    pathPrefix("files") {
      validateFile ~
      buildConfiguration ~
      uploadFile
    }
}
