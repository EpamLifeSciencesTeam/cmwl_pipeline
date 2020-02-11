package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import cats.instances.uuid
import cromwell.pipeline.datastorage.dto.{ ProjectId, UserId }
import cromwell.pipeline.datastorage.dto.project.{ ProjectAdditionRequest, ProjectDeleteRequest }
import cromwell.pipeline.service.{ ProjectDeactivationForbiddenException, ProjectNotFoundException, ProjectService }
import cromwell.pipeline.utils.auth.AccessTokenContent
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
            onComplete(projectService.getProjectByName(name, UserId(accessToken.userId))) {
              case Success(project) => complete(project)
              case Failure(e)       => complete(StatusCodes.NotFound, e.getMessage)
            }
          }
        },
        post {
          entity(as[ProjectAdditionRequest]) { request =>
            onComplete(projectService.addProject(request, UserId(accessToken.userId))) {
              case Success(_) => complete(StatusCodes.OK)
              case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
            }
          }
        },
        delete {
          entity(as[ProjectDeleteRequest]) {
            request =>
              onComplete(projectService.deactivateProjectById(request.projectId, UserId(accessToken.userId))) {
                case Success(Some(project)) => complete(project)
                case Failure(_: ProjectDeactivationForbiddenException) =>
                  complete(StatusCodes.Forbidden, ProjectController.PROJECT_DEACTIVATION_FORBIDDEN_MESSAGE)
                case Failure(_: ProjectNotFoundException) =>
                  complete(StatusCodes.NotFound, ProjectController.PROJECT_NOT_FOUND_MESSAGE)
                case _ => complete(StatusCodes.BadRequest)
              }
          }
        }
      )
    }
}

object ProjectController {
  val PROJECT_DEACTIVATION_FORBIDDEN_MESSAGE = "Project deactivation is allowed for owners only"
  val PROJECT_NOT_FOUND_MESSAGE = "Project not found"
}
