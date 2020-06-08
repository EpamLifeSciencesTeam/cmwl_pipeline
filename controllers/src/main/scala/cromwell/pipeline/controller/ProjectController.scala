package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.datastorage.dto.{ ProjectAdditionRequest, ProjectDeleteRequest, ProjectUpdateRequest }
import cromwell.pipeline.datastorage.formatters.ProjectFormatters._
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.service.Exceptions.{ ProjectAccessDeniedException, ProjectNotFoundException }
import cromwell.pipeline.service.{ ProjectService, VersioningException }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

class ProjectController(projectService: ProjectService)(
  implicit executionContext: ExecutionContext
) {
  val route: AccessTokenContent => Route = accessToken =>
    path("projects") {
      concat(
        get {
          parameter('name.as[String]) { name =>
            onComplete(projectService.getProjectByName(name, accessToken.userId)) {
              case Success(project)                         => complete(project)
              case Failure(e: ProjectNotFoundException)     => complete(StatusCodes.NotFound, e.getMessage)
              case Failure(e: ProjectAccessDeniedException) => complete(StatusCodes.Forbidden, e.getMessage)
              case Failure(e)                               => complete(StatusCodes.InternalServerError, e.getMessage)
            }
          }
        },
        post {
          entity(as[ProjectAdditionRequest]) { request =>
            onComplete(projectService.addProject(request, accessToken.userId)) {
              case Success(projectId) =>
                projectId match {
                  case Right(_)                     => complete(StatusCodes.OK)
                  case Left(e: VersioningException) => complete(StatusCodes.InternalServerError, e.getMessage)
                }
              case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
            }
          }
        },
        delete {
          entity(as[ProjectDeleteRequest]) {
            request =>
              onComplete(projectService.deactivateProjectById(request.projectId, accessToken.userId)) {
                case Success(project)                         => complete(project)
                case Failure(e: ProjectNotFoundException)     => complete(StatusCodes.NotFound, e.getMessage)
                case Failure(e: ProjectAccessDeniedException) => complete(StatusCodes.Forbidden, e.getMessage)
                case Failure(e)                               => complete(StatusCodes.InternalServerError, e.getMessage)
              }
          }
        },
        put {
          entity(as[ProjectUpdateRequest]) { request =>
            onComplete(projectService.updateProject(request, accessToken.userId)) {
              case Success(_) => complete(StatusCodes.NoContent)
              case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
            }
          }
        }
      )
    }
}
