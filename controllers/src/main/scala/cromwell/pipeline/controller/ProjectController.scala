package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import cromwell.pipeline.datastorage.dto.UserId
import cromwell.pipeline.datastorage.dto.project.ProjectAdditionRequest
import cromwell.pipeline.service.{ ProjectAccessDeniedException, ProjectNotFoundException, ProjectService }
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
              case Success(project)                         => complete(project)
              case Failure(e: ProjectNotFoundException)     => complete(StatusCodes.NotFound, e.getMessage)
              case Failure(e: ProjectAccessDeniedException) => complete(StatusCodes.Forbidden, e.getMessage)
              case Failure(e)                               => complete(StatusCodes.InternalServerError, e.getMessage)
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
        }
      )
    }
}
