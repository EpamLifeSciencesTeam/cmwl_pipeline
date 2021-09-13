package cromwell.pipeline.controller

import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ ExceptionHandler, Route }
import cromwell.pipeline.controller.UserController.userServiceExceptionHandler
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.service.UserService
import cromwell.pipeline.service.UserService.Exceptions.UserServiceException
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

class UserController(userService: UserService) {

  private val getUser: Route = get {
    parameter('email.as[String]) { email =>
      complete(userService.getUsersByEmail(email))
    }
  }

  private def deactivateUser(implicit accessToken: AccessTokenContent): Route = delete {
    complete(userService.deactivateUserById(accessToken.userId))
  }

  private def updateUser(implicit accessToken: AccessTokenContent): Route = put {
    path("info") {
      entity(as[UserUpdateRequest]) { userUpdateRequest =>
        complete(StatusCodes.NoContent, userService.updateUser(accessToken.userId, userUpdateRequest))
      }
    }
  }

  private def updatePassword(implicit accessToken: AccessTokenContent): Route = put {
    path("password") {
      entity(as[PasswordUpdateRequest]) { passwordUpdateRequest =>
        complete(StatusCodes.NoContent, userService.updatePassword(accessToken.userId, passwordUpdateRequest))
      }
    }
  }

  val route: AccessTokenContent => Route = implicit accessToken =>
    handleExceptions(userServiceExceptionHandler) {
      pathPrefix("users") {
        getUser ~
        deactivateUser ~
        updateUser ~
        updatePassword
      }
    }
}

object UserController {
  def excToStatusCode(e: UserServiceException): StatusCode = e match {
    case _: UserService.Exceptions.NotFound      => StatusCodes.NotFound
    case _: UserService.Exceptions.AccessDenied  => StatusCodes.Forbidden
    case _: UserService.Exceptions.WrongPassword => StatusCodes.BadRequest
    case _: UserService.Exceptions.InternalError => StatusCodes.InternalServerError
  }

  val userServiceExceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: UserServiceException => complete(excToStatusCode(e), e.getMessage)
    case e                       => complete(StatusCodes.InternalServerError, e.getMessage)
  }
}
