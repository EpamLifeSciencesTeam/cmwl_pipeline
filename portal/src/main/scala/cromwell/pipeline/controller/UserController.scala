package cromwell.pipeline.controller

import akka.http.scaladsl.server.Directives.complete
import cromwell.pipeline.datastorage.dto.user.UserUpdateRequest
import cromwell.pipeline.service.UserService
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cromwell.pipeline.datastorage.dto.user.PasswordUpdateRequest
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

class UserController(userService: UserService)(implicit executionContext: ExecutionContext) {

  val route: Route =
    path("users" / Segment) { id =>
      concat(
        put {
          entity(as[UserUpdateRequest]) { request =>
            onComplete(userService.updateUser(id, request)) {
              case Success(count) => getStatus(count)
              case _              => complete(StatusCodes.InternalServerError)
            }
          }
        },
        put {
          entity(as[PasswordUpdateRequest]) { request =>
            onComplete(userService.updateUserPassword(id, request)) {
              case Success(count) => getStatus(count)
              case Failure(_)     => complete(StatusCodes.BadRequest, "Password doesn't match")
              case _              => complete(StatusCodes.InternalServerError)
            }
          }
        }
      )
    }

  private def getStatus(count: Int): Route =
    if (count == 1) complete(StatusCodes.NoContent)
    else complete(StatusCodes.BadRequest, "Specified ID doesn't exist")
}
