package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.datastorage.dto.{ ProjectConfiguration, ProjectId }
import cromwell.pipeline.service.ProjectConfigurationService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

class ProjectConfigurationController(projectConfigurationService: ProjectConfigurationService)(
  implicit val ec: ExecutionContext
) {

  private val addConfiguration: Route = put {
    entity(as[ProjectConfiguration]) { request =>
      onComplete(projectConfigurationService.addConfiguration(request)) {
        case Failure(e)            => complete(StatusCodes.InternalServerError, e.getMessage)
        case Success(updateResult) => complete(updateResult)
      }
    }
  }

  private val getConfiguration: Route = get {
    parameter('project_id.as[String]) { projectId =>
      onComplete(projectConfigurationService.getById(ProjectId(projectId))) {
        case Failure(e)                   => complete(StatusCodes.InternalServerError, e.getMessage)
        case Success(Some(configuration)) => complete(configuration)
        case Success(None) =>
          complete(StatusCodes.NotFound, s"There is no configuration with project_id: $projectId")
      }
    }
  }

  private val deactivateConfiguration: Route = delete {
    parameter('project_id.as[String]) { projectId =>
      onComplete(projectConfigurationService.deactivateConfiguration(ProjectId(projectId))) {
        case Failure(e)            => complete(StatusCodes.InternalServerError, e.getMessage)
        case Success(updateResult) => complete(updateResult)
      }
    }
  }

  val route: AccessTokenContent => Route = _ =>
    pathPrefix("configurations") {
      addConfiguration ~
      getConfiguration ~
      deactivateConfiguration
    }
}
