package cromwell.pipeline.controller

import java.nio.file.Paths

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cromwell.pipeline.datastorage.dto.ProjectBuildConfigurationRequest.ProjectUpdateFileRequest
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.datastorage.dto.{FileContent, PipelineVersion, ProjectBuildConfigurationRequest, ProjectId}
import cromwell.pipeline.service.{ProjectFileService, ProjectService, VersioningException}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ProjectFileController(wdlService: ProjectFileService, projectService: ProjectService)(
  implicit val executionContext: ExecutionContext
) {
  val route: AccessTokenContent => Route = _ =>
    concat(
      path("files" / "validation") {
        post {
          entity(as[FileContent]) { request =>
            onComplete(wdlService.validateFile(request)) {
              case Success(Left(e)) => complete(StatusCodes.Conflict, e.errors)
              case Success(_)       => complete(StatusCodes.OK)
              case Failure(e)       => complete(StatusCodes.InternalServerError, e.getMessage)
            }
          }
        }
      },
      path("files") {
        concat(
          post {
            entity(as[ProjectUpdateFileRequest]) {
              request =>
                onComplete(for {
                  validateResponse <- wdlService.validateFile(FileContent(request.projectFile.content))
                  uploadResponse <- wdlService.uploadFile(request.project, request.projectFile, request.version)
                } yield {
                  (validateResponse, uploadResponse) match {
                    case (Right(_), Right(response)) => StatusCodes.OK.intValue -> response
                    case (Left(_), Right(response))  => StatusCodes.Created.intValue -> response
                    case (_, Left(response))         => StatusCodes.UnprocessableEntity.intValue -> response.message
                  }
                }) {
                  case Success((status, message)) => complete(status, message)
                  case Failure(e)                 => complete(StatusCodes.InternalServerError, e.getMessage)
                }
            }
          },
          get {
            parameter('projectId.as[String], 'path.as[String], 'version.as[String]) {
              (projectId, path, version) =>
                onComplete(projectService.getProjectById(ProjectId(projectId)).flatMap {
                  case Some(project) => wdlService.getFile(project, Paths.get(path), Some(PipelineVersion(version)))
                  case None =>
                    Future.successful(Left(VersioningException(s"Project with ID $projectId does not exist")))
                }) {
                  case Success(Left(e)) => complete(StatusCodes.NotFound, e.getMessage)
                  case Success(_)       => complete(StatusCodes.OK)
                  case Failure(e)       => complete(StatusCodes.InternalServerError, e.getMessage)
                }
            }
          },
          delete {
            parameter('projectId.as[String], 'path.as[String], 'branchName.as[String], 'commitMessage.as[String]) {
              (projectId, path, branchName, commitMessage) =>
                onComplete(projectService.getProjectById(ProjectId(projectId)).flatMap {
                  case Some(project) =>
                    val future: Future[Either[VersioningException, String]] =
                      wdlService.deleteFile(project, Paths.get(path), branchName, commitMessage)
                    future
                  case None =>
                    val future: Future[Either[VersioningException, String]] =
                      Future.successful(Left(VersioningException(s"Project with ID $projectId does not exist")))
                    future
                }) {
                  case Success(Left(e)) => complete(StatusCodes.NotFound, e.getMessage)
                  case Success(_)       => complete(StatusCodes.OK)
                  case Failure(e)       => complete(StatusCodes.InternalServerError, e.getMessage)
                }
            }
          }
        )
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
