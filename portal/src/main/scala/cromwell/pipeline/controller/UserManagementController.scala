package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{as, complete, delete, entity, onComplete, path, pathPrefix}
import akka.http.scaladsl.server.Route
import cromwell.pipeline.datastorage.dto.management.DeactivateUserRequest
import cromwell.pipeline.service.UserManagementService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._



import scala.concurrent.ExecutionContext
import scala.util.Success

class UserManagementController(userService: UserManagementService) (implicit executionContext: ExecutionContext) {

  val route: Route = pathPrefix("management") {
      path("deactivate" )
        delete {
          entity(as[DeactivateUserRequest]) { request =>
            onComplete(userService.deactivate(request)) {
              case Success(_) => complete(StatusCodes.NoContent)
              case _                           => complete(StatusCodes.BadRequest)
            }
          }
        }
  }

}
