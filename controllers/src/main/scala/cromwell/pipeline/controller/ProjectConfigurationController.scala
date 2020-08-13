package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.datastorage.dto.{ ProjectConfiguration, ProjectId }
import cromwell.pipeline.service.ProjectConfigurationService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import cromwell.pipeline.datastorage.formatters.ProjectFormatters._

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

class ProjectConfigurationController(projectConfigurationService: ProjectConfigurationService)(
  implicit val ec: ExecutionContext
) {
  val route: AccessTokenContent => Route = _ =>
    path("configurations") {
      concat(
        put {
          entity(as[ProjectConfiguration]) { request =>
            onComplete(projectConfigurationService.addConfiguration(request)) {
              case Failure(e)            => complete(StatusCodes.InternalServerError, e.getMessage)
              case Success(updateResult) => complete(updateResult)
            }
          }
        },
        get {
          concat(
            parameter('project_id.as[String]) { projectId =>
              onComplete(projectConfigurationService.getById(ProjectId(projectId))) {
                case Failure(e)                   => complete(StatusCodes.InternalServerError, e.getMessage)
                case Success(Some(configuration)) => complete(configuration)
                case Success(None) =>
                  complete(StatusCodes.NotFound, s"There is no configuration with project_id: $projectId")
              }
            }
          )
        }
      )
    }
}
