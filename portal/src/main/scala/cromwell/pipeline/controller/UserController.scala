package cromwell.pipeline.controller

import akka.http.scaladsl.server.Directives.{complete}
import cromwell.pipeline.datastorage.dto.user.UpdateRequest
import cromwell.pipeline.service.UserService
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.concurrent.ExecutionContext
import scala.util.Success

class UserController(userService: UserService) (implicit executionContext: ExecutionContext) {


  val route: Route =
    concat(
      path("users"  / Segment) { id =>
          put {
            entity(as[UpdateRequest]) { request =>
              onComplete(userService.update(id,request)) {
                case Success(1) => complete(StatusCodes.NoContent)
                case _          => complete(StatusCodes.InternalServerError)
              }
            }
          }
      }
    )
}