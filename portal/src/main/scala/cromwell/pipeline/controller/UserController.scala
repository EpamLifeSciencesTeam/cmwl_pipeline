package cromwell.pipeline.controller

import akka.http.scaladsl.server.Directives.complete
import cromwell.pipeline.datastorage.dto.user.UserUpdateRequest
import cromwell.pipeline.service.UserService
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cromwell.pipeline.datastorage.dto.user.PasswordUpdateRequest
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.concurrent.{ ExecutionContext }
import scala.util.Success

class UserController(userService: UserService)(implicit executionContext: ExecutionContext) {

  val route: Route =
    path("users" / Segment) { id =>
      concat(
        put {
          entity(as[UserUpdateRequest]) { request =>
            onComplete(userService.updateUser(id, request)) {
              case Success(count) => userService.getStatus(count)
              case _              => getDbErrorStatus()
            }
          }
        },
        put {
          entity(as[PasswordUpdateRequest]) { request =>
            onComplete(userService.updateUserPassword(id, request)) {
              case Success(count) => userService.getStatus(count)
              case _              => getDbErrorStatus()
            }
          }
        }
      )
    }

  private def getDbErrorStatus() = complete(StatusCodes.InternalServerError, "Database can't handle query")
}
