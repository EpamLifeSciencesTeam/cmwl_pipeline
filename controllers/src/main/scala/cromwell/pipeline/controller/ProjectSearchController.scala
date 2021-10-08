package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ ExceptionHandler, Route }
import cromwell.pipeline.controller.ProjectSearchController.projectSearchExceptionHandler
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.service.ProjectSearchService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

class ProjectSearchController(projectSearchService: ProjectSearchService) {

  private def searchProjects(implicit accessToken: AccessTokenContent): Route = post {
    pathEndOrSingleSlash {
      entity(as[ProjectSearchRequest]) { request =>
        complete(projectSearchService.searchProjects(request, accessToken.userId))
      }
    }

  }

  val route: AccessTokenContent => Route = implicit accessToken =>
    handleExceptions(projectSearchExceptionHandler) {
      pathPrefix("projects" / "search") {
        searchProjects
      }
    }
}

object ProjectSearchController {

  val projectSearchExceptionHandler: ExceptionHandler = ExceptionHandler {
    case e => complete(StatusCodes.InternalServerError, e.getMessage)
  }
}
