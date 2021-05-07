package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{ entity, _ }
import akka.http.scaladsl.server.Route
import cromwell.pipeline.controller.utils.FromStringUnmarshallers._
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.datastorage.dto.{ RunCreateRequest, RunDeleteRequest, RunUpdateRequest }
import cromwell.pipeline.model.wrapper.RunId
import cromwell.pipeline.service.RunService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.util.{ Failure, Success }

class RunController(runService: RunService) {

  private def getRun(implicit accessToken: AccessTokenContent): Route = get {
    parameter('run_id.as[RunId]) { runId =>
      {
        onComplete(runService.getRunByIdAndUser(runId, accessToken.userId)) {
          case Success(Some(response)) => complete(response)
          case Success(None)           => complete(StatusCodes.NotFound, "Run not found")
          case Failure(_)              => complete(StatusCodes.InternalServerError, "Internal error")
        }
      }
    }
  }

  private def deleteRun(implicit accessToken: AccessTokenContent): Route = delete {
    entity(as[RunDeleteRequest]) { request =>
      onComplete(runService.deleteRunById(request.runId, accessToken.userId)) {
        case Success(idResponse) => complete(idResponse)
        case Failure(_)          => complete(StatusCodes.InternalServerError, "Internal error")
      }
    }
  }

  private def updateRun(implicit accessToken: AccessTokenContent): Route = put {
    entity(as[RunUpdateRequest]) { runUpdateRequest =>
      onComplete(runService.updateRun(runUpdateRequest, accessToken.userId)) {
        case Success(_)   => complete(StatusCodes.NoContent)
        case Failure(exc) => complete(StatusCodes.InternalServerError, exc.getMessage)
      }
    }
  }

  private val addRun: Route = post {
    entity(as[RunCreateRequest]) { request =>
      onComplete(runService.addRun(request)) {
        case Success(response) => complete(response)
        case Failure(_)        => complete(StatusCodes.InternalServerError, "Internal error")
      }
    }
  }

  val route: AccessTokenContent => Route = implicit accessToken =>
    pathPrefix("runs") {
      getRun ~
      deleteRun ~
      updateRun ~
      addRun
    }
}
