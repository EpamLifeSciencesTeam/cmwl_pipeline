package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ ExceptionHandler, Route }
import cromwell.pipeline.controller.ProjectSearchController.projectSearchExceptionHandler
import cromwell.pipeline.controller.utils.PathMatchers.ProjectSearchFilterId
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.service.ProjectSearchService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

class ProjectSearchController(projectSearchService: ProjectSearchService) {

  private def searchProjectsByNewFilter(implicit accessToken: AccessTokenContent): Route = post {
    pathEndOrSingleSlash {
      entity(as[ProjectSearchRequest]) { request =>
        complete(projectSearchService.searchProjectsByNewQuery(request, accessToken.userId))
      }
    }
  }

  private def searchProjectsBySearchId(implicit accessToken: AccessTokenContent): Route = get {
    path(ProjectSearchFilterId) { searchId =>
      complete(projectSearchService.searchProjectsByFilterId(searchId, accessToken.userId))
    }
  }

  val route: AccessTokenContent => Route = implicit accessToken =>
    handleExceptions(projectSearchExceptionHandler) {
      pathPrefix("projects" / "search") {
        searchProjectsByNewFilter ~
        searchProjectsBySearchId
      }
    }
}

object ProjectSearchController {

  val projectSearchExceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: ProjectSearchService.Exceptions.NotFound => complete(StatusCodes.NotFound, e.getMessage)
    case e                                           => complete(StatusCodes.InternalServerError, e.getMessage)
  }
}
