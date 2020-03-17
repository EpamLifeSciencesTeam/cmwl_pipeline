package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.datastorage.utils.auth.AccessTokenContent
import cromwell.pipeline.service.UserService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

class UserController(userService: UserService)(implicit executionContext: ExecutionContext) {

  val route: AccessTokenContent => Route = accessToken =>
    path("users") {
      concat(
        get {
          parameter('email.as[String]) { email =>
            onComplete(userService.getUsersByEmail(email)) {
              case Success(r) => complete(r)
              case Failure(exc) =>
                complete(StatusCodes.InternalServerError, exc.getMessage)
            }
          }
        },
        delete {
          onComplete(userService.deactivateUserById(accessToken.userId)) {
            case Success(Some(idResponse)) => complete(idResponse)
            case Success(None)             => complete(StatusCodes.NotFound, "User not found")
            case Failure(_)                => complete(StatusCodes.InternalServerError, "Internal error")
          }
        },
        put {
          entity(as[UserUpdateRequest]) { userUpdateRequest =>
            onComplete(userService.updateUser(accessToken.userId, userUpdateRequest)) {
              case Success(_)   => complete(StatusCodes.NoContent)
              case Failure(exc) => complete(StatusCodes.InternalServerError, exc.getMessage)
            }
          }
        },
        put {
          entity(as[PasswordUpdateRequest]) { passwordUpdateRequest =>
            onComplete(userService.updatePassword(accessToken.userId, passwordUpdateRequest)) {
              case Success(_)   => complete(StatusCodes.NoContent)
              case Failure(exc) => complete(StatusCodes.BadRequest, exc.getMessage)
            }
          }
        }
      )
    }
}
