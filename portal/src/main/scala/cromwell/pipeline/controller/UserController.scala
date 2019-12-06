package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{ as, complete, delete, entity, onComplete, path, pathPrefix, _ }
import akka.http.scaladsl.server.Route
import cromwell.pipeline.datastorage.dto.UserId
import cromwell.pipeline.datastorage.dto.user.DeactivateUserRequestByEmail
import cromwell.pipeline.service.UserService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.concurrent.ExecutionContext
import scala.util.Success

class UserController(userService: UserService)(implicit executionContext: ExecutionContext) {

  val route: Route = pathPrefix("users") {
    concat(
      path("deactivate") {
        delete {
          entity(as[DeactivateUserRequestByEmail]) { request =>
            onComplete(userService.deactivateByEmail(request)) {
              case Success(Some(emailResponse)) => complete(emailResponse)
              case _                            => complete(StatusCodes.BadRequest)
            }
          }
        }
      },
      path("deactivate" / ".{36}".r) { userId =>
        delete {
          onComplete(userService.deactivateById(UserId(userId))) {
            case Success(Some(idResponse)) => complete(idResponse)
            case _                         => complete(StatusCodes.BadRequest)
          }
        }
      }
    )
  }
}
