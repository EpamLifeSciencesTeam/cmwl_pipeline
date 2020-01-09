package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.JavaUUID
import akka.http.scaladsl.server.Route
import cromwell.pipeline.datastorage.dto.{ ProjectCreationRequest, ProjectId, UserId }
import cromwell.pipeline.service.{ ProjectDeactivationForbiddenException, ProjectNotFoundException, ProjectService }
import cromwell.pipeline.utils.auth.AccessTokenContent
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.concurrent.ExecutionContext
import scala.language.{ implicitConversions, postfixOps }
import scala.util.{ Failure, Success }

class ProjectController(projectService: ProjectService)(implicit executionContext: ExecutionContext) {
  import ProjectController._

  val route: AccessTokenContent => Route = accessToken =>
    pathPrefix("projects") {
      post {
        entity(as[ProjectCreationRequest]) { request =>
          onComplete(projectService.addProject(request)) {
            case Success(projectId) => complete(projectId)
            case _                  => complete(StatusCodes.InternalServerError)
          }
        }
      } ~
        path(JavaUUID) { uuid =>
          get {
            onComplete(projectService.getProjectById(ProjectId(uuid.toString))) {
              case Success(Some(project)) => complete(project)
              case Success(None)          => complete(StatusCodes.NotFound, PROJECT_NOT_FOUND_MESSAGE)
              case _                      => complete(StatusCodes.BadRequest)
            }
          } ~
            delete {
              onComplete(projectService.deactivateProjectById(ProjectId(uuid.toString), UserId(accessToken.userId))) {
                case Success(Some(project)) => complete(project)
                case Failure(_: ProjectDeactivationForbiddenException) =>
                  complete(StatusCodes.Forbidden, PROJECT_DEACTIVATION_FORBIDDEN_MESSAGE)
                case Failure(_: ProjectNotFoundException) => complete(StatusCodes.NotFound, PROJECT_NOT_FOUND_MESSAGE)
                case _                                    => complete(StatusCodes.BadRequest)
              }
            }
        }
    }
}

object ProjectController {
  val PROJECT_DEACTIVATION_FORBIDDEN_MESSAGE = "Project deactivation is allowed for owners only"
  val PROJECT_NOT_FOUND_MESSAGE = "Project not found"
}
