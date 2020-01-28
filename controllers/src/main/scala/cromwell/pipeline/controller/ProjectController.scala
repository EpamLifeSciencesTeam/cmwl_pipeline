package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import cromwell.pipeline.datastorage.dto.project.ProjectAdditionRequest
import cromwell.pipeline.service.ProjectService
import cromwell.pipeline.utils.auth.AccessTokenContent
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

class ProjectController(projectService: ProjectService)(
  implicit executionContext: ExecutionContext
) {

  val route: AccessTokenContent => Route = accessToken =>
    path("projects") {
      get {
        parameter('name.as[String]) { name =>
          onComplete(projectService.getProjectByName(name)) {
            case Success(value) => complete(value)
            case Failure(e)     => complete(StatusCodes.InternalServerError, e.getMessage)
          }
        }
      }
    }

}