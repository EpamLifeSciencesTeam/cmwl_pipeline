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

  private def addConfiguration(implicit accessToken: AccessTokenContent): Route = put {
    entity(as[ProjectConfiguration]) { request =>
      onComplete(projectConfigurationService.addConfiguration(request, accessToken.userId)) {
        case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
        case Success(_) => complete(StatusCodes.OK)
      }
    }
  }

  private def getConfiguration(implicit accessToken: AccessTokenContent): Route = get {
    parameter('project_id.as[String]) { projectId =>
      onComplete(projectConfigurationService.getLastByProjectId(ProjectId(projectId), accessToken.userId)) {
        case Failure(e)                   => complete(StatusCodes.InternalServerError, e.getMessage)
        case Success(Some(configuration)) => complete(configuration)
        case Success(None) =>
          complete(StatusCodes.NotFound, s"There is no configuration with project_id: $projectId")
      }
    }
  }

  private def deactivateConfiguration(implicit accessToken: AccessTokenContent): Route = delete {
    parameter('project_id.as[String]) { projectId =>
      onComplete(projectConfigurationService.deactivateLastByProjectId(ProjectId(projectId), accessToken.userId)) {
        case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
        case Success(_) => complete(StatusCodes.NoContent)
      }
    }
  }

  val route: AccessTokenContent => Route = implicit accessToken =>
    pathPrefix("configurations") {
      addConfiguration ~
      getConfiguration ~
      deactivateConfiguration
    }
}
