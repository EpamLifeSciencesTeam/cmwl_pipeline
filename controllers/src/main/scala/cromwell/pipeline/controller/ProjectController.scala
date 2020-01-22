package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cromwell.pipeline.service.ProjectService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

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
