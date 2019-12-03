package cromwell.pipeline.controller


import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{as, complete, delete, entity, onComplete, path, pathPrefix, _}
import akka.http.scaladsl.server.Route
import cromwell.pipeline.datastorage.dto.UserId
import cromwell.pipeline.datastorage.dto.management.DeactivateUserRequestByEmail
import cromwell.pipeline.service.UserManagementService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.concurrent.ExecutionContext
import scala.util.Success

class UserController(userService: UserManagementService)(implicit executionContext: ExecutionContext) {

  val route: Route = pathPrefix("users") {
     concat(
     path("deactivate") {
        delete {
          entity(as[DeactivateUserRequestByEmail]) { request =>
            onComplete(userService.deactivateByEmail(request)) {
              case Success(_) => complete(StatusCodes.NoContent)
              case _ => complete(StatusCodes.BadRequest)
            }
          }
        }
      },
      path("deactivate" / ".{36}".r) { userId =>
        delete {
            onComplete(userService.deactivateById(UserId(userId))) {
              case Success(_) => complete(StatusCodes.NoContent)
              case _ => complete(StatusCodes.BadRequest)
            }
        }
      })
  }
}
