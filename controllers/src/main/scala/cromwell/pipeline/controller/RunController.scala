package cromwell.pipeline.controller

import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.Directives.{ entity, _ }
import akka.http.scaladsl.server.{ ExceptionHandler, Route }
import cromwell.pipeline.controller.RunController.runServiceExceptionHandler
import cromwell.pipeline.controller.utils.PathMatchers.{ RunId, ProjectId => ProjectIdPM }
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.model.wrapper.ProjectId
import cromwell.pipeline.service.RunService
import cromwell.pipeline.service.RunService.Exceptions.RunServiceException
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

class RunController(runService: RunService) {

  private def getRun(projectId: ProjectId)(implicit accessToken: AccessTokenContent): Route = get {
    path(RunId) { runId =>
      complete(runService.getRunByIdAndUser(runId, projectId, accessToken.userId))
    }
  }
  private def getRunsByProject(projectId: ProjectId)(implicit accessToken: AccessTokenContent): Route = get {
    pathEndOrSingleSlash {
      complete(runService.getRunsByProject(projectId, accessToken.userId))
    }
  }

  private def deleteRun(projectId: ProjectId)(implicit accessToken: AccessTokenContent): Route = delete {
    path(RunId) { runId =>
      complete(runService.deleteRunById(runId, projectId, accessToken.userId))
    }
  }

  private def updateRun(projectId: ProjectId)(implicit accessToken: AccessTokenContent): Route = put {
    path(RunId) { runId =>
      entity(as[RunUpdateRequest]) { runUpdateRequest =>
        complete(StatusCodes.NoContent, runService.updateRun(runId, runUpdateRequest, projectId, accessToken.userId))
      }
    }
  }

  private def addRun(projectId: ProjectId)(implicit accessToken: AccessTokenContent): Route = post {
    entity(as[RunCreateRequest]) { request =>
      complete(runService.addRun(request, projectId, accessToken.userId))
    }
  }

  val route: AccessTokenContent => Route = implicit accessToken =>
    handleExceptions(runServiceExceptionHandler) {
      pathPrefix("projects" / ProjectIdPM / "runs") { projectId =>
        getRun(projectId) ~
        getRunsByProject(projectId) ~
        deleteRun(projectId) ~
        updateRun(projectId) ~
        addRun(projectId)
      }
    }
}

object RunController {
  def excToStatusCode(e: RunServiceException): StatusCode = e match {
    case _: RunService.Exceptions.AccessDenied  => StatusCodes.Forbidden
    case _: RunService.Exceptions.NotFound      => StatusCodes.NotFound
    case _: RunService.Exceptions.InternalError => StatusCodes.InternalServerError
  }

  val runServiceExceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: RunServiceException => complete(excToStatusCode(e), e.getMessage)
    case e                      => complete(StatusCodes.InternalServerError, e.getMessage)
  }
}
