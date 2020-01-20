package cromwell.pipeline.controller

import akka.http.scaladsl.server.Directives.{ complete, concat, get, onComplete, parameter, path }

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cromwell.pipeline.datastorage.dto.UserId
import cromwell.pipeline.service.UserService
import cromwell.pipeline.utils.auth.AccessTokenContent
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
          onComplete(userService.deactivateUserById(UserId(accessToken.userId))) {
            case Success(Some(idResponse)) => complete(idResponse)
            case Success(None)             => complete(StatusCodes.NotFound, "User not found")
            case Failure(_)                => complete(StatusCodes.InternalServerError, "Internal error")
          }
        }
      )
    }
}
