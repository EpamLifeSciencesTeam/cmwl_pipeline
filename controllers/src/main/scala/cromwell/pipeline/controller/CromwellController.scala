package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.service.CromwellService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

class CromwellController(cromwellBackendService: CromwellService)(
  implicit executionContext: ExecutionContext
) {

  val route: AccessTokenContent => Route = _ => {
    path("cromwell" / "engine" / "status") {
      concat(
        get {
          onComplete(cromwellBackendService.getEngineStatus()) {
            case Success(Right(state)) => complete(StatusCodes.OK, state)
            case Success(Left(exception)) => complete(StatusCodes.UnprocessableEntity, exception.getMessage)
            case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
          }
        }
      )
    }
  }

}
