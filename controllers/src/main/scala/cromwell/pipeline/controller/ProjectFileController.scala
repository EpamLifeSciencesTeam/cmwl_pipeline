package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cromwell.pipeline.datastorage.dto.SuccessResponseMessage
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.datastorage.formatters.ProjectFormatters.{
  projectConfigurationFormat,
  projectUpdateFileRequestFormat,
  validateFileRequestFormat
}
import cromwell.pipeline.service.VersioningException.{
  FileException,
  GitException,
  HttpException,
  ProjectException,
  RepositoryException
}
import cromwell.pipeline.datastorage.dto.{
  ProjectBuildConfigurationRequest,
  ProjectFileContent,
  ProjectUpdateFileRequest
}
import cromwell.pipeline.service.{ ProjectFileService }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.concurrent.{ ExecutionContext }
import scala.util.{ Failure, Success }

class ProjectFileController(wdlService: ProjectFileService)(implicit val executionContext: ExecutionContext) {

  val route: AccessTokenContent => Route = _ =>
    concat(
      path("files" / "validation") {
        post {
          entity(as[ProjectFileContent]) { request =>
            onComplete(wdlService.validateFile(request)) {
              case Success(Left(e)) => complete(StatusCodes.Conflict, e.errors)
              case Success(_)       => complete(StatusCodes.OK)
              case Failure(e)       => complete(StatusCodes.InternalServerError, e.getMessage)
            }
          }
        }
      },
      path("files") {
        post {
          entity(as[ProjectUpdateFileRequest]) {
            request =>
              onComplete(for {
                validateResponse <- wdlService.validateFile(request.projectFile.content)
                uploadResponse <- wdlService.uploadFile(request.project, request.projectFile, request.version)
              } yield {
                (validateResponse, uploadResponse) match {
                  case (Right(_), Right(responseMessage)) => StatusCodes.OK.intValue -> responseMessage
                  case (Left(_), Right(responseMessage))  => StatusCodes.Created.intValue -> responseMessage
                  case (_, Left(response)) =>
                    response match {
                      case HttpException(message)       => StatusCodes.UnprocessableEntity.intValue -> message
                      case FileException(message)       => StatusCodes.UnprocessableEntity.intValue -> message
                      case GitException(message)        => StatusCodes.UnprocessableEntity.intValue -> message
                      case RepositoryException(message) => StatusCodes.UnprocessableEntity.intValue -> message
                      case ProjectException(message)    => StatusCodes.UnprocessableEntity.intValue -> message
                    }
                }
              }) {
                case Success((status, p @ SuccessResponseMessage(_))) =>
                  complete((status, p))
                case Success((status, message)) => complete((status, s"File have not uploaded due to $message"))
                case Failure(e)                 => complete(StatusCodes.InternalServerError, e.getMessage)
              }
          }
        }
      },
      path("files" / "configurations") {
        post {
          entity(as[ProjectBuildConfigurationRequest]) { request =>
            onComplete(wdlService.buildConfiguration(request.projectId, request.projectFile)) {
              case Success(Left(e))              => complete(StatusCodes.UnprocessableEntity, e.errors)
              case Success(Right(configuration)) => complete(configuration)
              case Failure(e)                    => complete(StatusCodes.InternalServerError, e.getMessage)
            }
          }
        }
      }
    )
}
