package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cromwell.pipeline.controller.utils.FromStringUnmarshallers._
import cromwell.pipeline.controller.utils.PathMatchers.{ Path, ProjectId }
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.service.ProjectConfigurationService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

class ProjectConfigurationController(projectConfigurationService: ProjectConfigurationService)(
  implicit val ec: ExecutionContext
) {

  private def addConfiguration(projectId: ProjectId)(implicit accessToken: AccessTokenContent): Route = put {
    entity(as[ProjectConfigurationAdditionRequest]) { request =>
      val newProjectConfiguration = ProjectConfiguration(
        id = request.id,
        projectId = projectId,
        active = request.active,
        projectFileConfigurations = request.projectFileConfigurations,
        version = request.version
      )
      onComplete(projectConfigurationService.addConfiguration(newProjectConfiguration, accessToken.userId)) {
        case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
        case Success(_) => complete(StatusCodes.OK)
      }
    }
  }

  private def getConfiguration(projectId: ProjectId)(implicit accessToken: AccessTokenContent): Route = get {
    onComplete(projectConfigurationService.getLastByProjectId(projectId, accessToken.userId)) {
      case Failure(e)                   => complete(StatusCodes.InternalServerError, e.getMessage)
      case Success(Some(configuration)) => complete(configuration)
      case Success(None) =>
        complete(StatusCodes.NotFound, s"There is no configuration with project_id: ${projectId.value}")
    }
  }

  private def deactivateConfiguration(projectId: ProjectId)(implicit accessToken: AccessTokenContent): Route = delete {
    onComplete(projectConfigurationService.deactivateLastByProjectId(projectId, accessToken.userId)) {
      case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
      case Success(_) => complete(StatusCodes.NoContent)
    }
  }

  private def buildConfiguration(projectId: ProjectId)(implicit accessToken: AccessTokenContent): Route = get {
    path("files" / Path) { projectFilePath =>
      parameters('version.as[PipelineVersion].optional) { version =>
        onComplete(
          projectConfigurationService.buildConfiguration(
            projectId,
            projectFilePath,
            version,
            accessToken.userId
          )
        ) {
          case Success(configuration)        => complete(configuration)
          case Failure(ValidationError(msg)) => complete(StatusCodes.UnprocessableEntity, msg)
          case Failure(e)                    => complete(StatusCodes.InternalServerError, e.getMessage)
        }
      }
    }
  }

  val route: AccessTokenContent => Route = implicit accessToken =>
    pathPrefix("projects" / ProjectId / "configurations") { projectId =>
      buildConfiguration(projectId) ~
      addConfiguration(projectId) ~
      getConfiguration(projectId) ~
      deactivateConfiguration(projectId)
    }
}
