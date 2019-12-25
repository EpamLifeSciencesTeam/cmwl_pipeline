package cromwell.pipeline.controller

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
    pathPrefix("users") {
      path("delete") {
        delete {
          onComplete(userService.deactivateById(UserId(accessToken.userId))) {
            case Success(Some(idResponse)) => complete(idResponse)
            case Success(None)             => complete(StatusCodes.NotFound, "User not found")
            case Failure(_)                => complete(StatusCodes.InternalServerError, "Internal error")
          }
        }
      }
    }
}
