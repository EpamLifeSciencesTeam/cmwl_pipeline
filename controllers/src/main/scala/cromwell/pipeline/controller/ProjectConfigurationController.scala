package cromwell.pipeline.controller

import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ ExceptionHandler, Route }
import cromwell.pipeline.controller.ProjectConfigurationController._
import cromwell.pipeline.controller.utils.FromStringUnmarshallers._
import cromwell.pipeline.controller.utils.FromUnitMarshaller._
import cromwell.pipeline.controller.utils.PathMatchers.{ Path, ProjectId }
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.service.ProjectConfigurationService
import cromwell.pipeline.service.ProjectConfigurationService.Exceptions._
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.concurrent.ExecutionContext

class ProjectConfigurationController(projectConfigurationService: ProjectConfigurationService)(
  implicit val ec: ExecutionContext
) {

  private def addConfiguration(projectId: ProjectId)(implicit accessToken: AccessTokenContent): Route = put {
    entity(as[ProjectConfigurationAdditionRequest]) { request =>
      val newProjectConfiguration = ProjectConfiguration(
        id = request.id,
        projectId = projectId,
        active = request.active,
        wdlParams = request.wdlParams,
        version = request.version
      )
      complete(
        projectConfigurationService.addConfiguration(newProjectConfiguration, accessToken.userId)
      )
    }
  }

  private def getConfiguration(projectId: ProjectId)(implicit accessToken: AccessTokenContent): Route = get {
    complete(projectConfigurationService.getLastByProjectId(projectId, accessToken.userId))
  }

  private def deactivateConfiguration(projectId: ProjectId)(implicit accessToken: AccessTokenContent): Route = delete {
    complete(
      projectConfigurationService.deactivateLastByProjectId(projectId, accessToken.userId)
    )
  }

  private def buildConfiguration(projectId: ProjectId)(implicit accessToken: AccessTokenContent): Route = get {
    path("files" / Path) { projectFilePath =>
      parameters('version.as[PipelineVersion].optional) { version =>
        complete(
          projectConfigurationService.buildConfiguration(
            projectId,
            projectFilePath,
            version,
            accessToken.userId
          )
        )
      }
    }
  }

  val route: AccessTokenContent => Route = implicit accessToken =>
    handleExceptions(projectConfigurationServiceExceptionHandler) {
      pathPrefix("projects" / ProjectId / "configurations") { projectId =>
        buildConfiguration(projectId) ~
        addConfiguration(projectId) ~
        getConfiguration(projectId) ~
        deactivateConfiguration(projectId)
      }
    }
}

object ProjectConfigurationController {
  def excToStatusCode(e: ProjectConfigurationServiceException): StatusCode = e match {
    case _: ProjectConfigurationService.Exceptions.NotFound        => StatusCodes.NotFound
    case _: ProjectConfigurationService.Exceptions.AccessDenied    => StatusCodes.Forbidden
    case _: ProjectConfigurationService.Exceptions.InternalError   => StatusCodes.InternalServerError
    case _: ProjectConfigurationService.Exceptions.ValidationError => StatusCodes.UnprocessableEntity
  }

  val projectConfigurationServiceExceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: ProjectConfigurationServiceException => complete(excToStatusCode(e), e.getMessage)
    case e                                       => complete(StatusCodes.InternalServerError, e.getMessage)
  }
}
