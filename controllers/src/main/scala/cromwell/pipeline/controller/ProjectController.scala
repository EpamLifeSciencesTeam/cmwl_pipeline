package cromwell.pipeline.controller

import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ ExceptionHandler, Route }
import cromwell.pipeline.controller.ProjectController.projectServiceExceptionHandler
import cromwell.pipeline.controller.utils.PathMatchers.ProjectId
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.datastorage.dto.{ ProjectAdditionRequest, ProjectUpdateNameRequest }
import cromwell.pipeline.service.ProjectService
import cromwell.pipeline.service.ProjectService.Exceptions.ProjectServiceException
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

class ProjectController(projectService: ProjectService) {

  private def getProjectById(implicit accessToken: AccessTokenContent): Route = get {
    path(ProjectId) { projectId =>
      complete(projectService.getUserProjectById(projectId, accessToken.userId))
    }
  }

  private def getProjectByName(implicit accessToken: AccessTokenContent): Route = get {
    pathEndOrSingleSlash {
      parameter('name.as[String]) { name =>
        complete(projectService.getUserProjectByName(name, accessToken.userId))
      }
    }
  }

  private def getProjects(implicit accessToken: AccessTokenContent): Route = get {
    pathEndOrSingleSlash {
      complete(projectService.getUserProjects(accessToken.userId))
    }
  }

  private def addProject(implicit accessToken: AccessTokenContent): Route = post {
    entity(as[ProjectAdditionRequest]) { request =>
      complete(projectService.addProject(request, accessToken.userId))
    }
  }

  private def deactivateProject(implicit accessToken: AccessTokenContent): Route = path(ProjectId) { projectId =>
    delete {
      complete(projectService.deactivateProjectById(projectId, accessToken.userId))
    }
  }

  private def updateProject(implicit accessToken: AccessTokenContent): Route = path(ProjectId) { projectId =>
    put {
      entity(as[ProjectUpdateNameRequest]) { request =>
        complete(projectService.updateProjectName(projectId, request, accessToken.userId))
      }
    }
  }

  val route: AccessTokenContent => Route = implicit accessToken =>
    handleExceptions(projectServiceExceptionHandler) {
      pathPrefix("projects") {
        getProjectById ~
        getProjectByName ~
        getProjects ~
        addProject ~
        deactivateProject ~
        updateProject
      }
    }
}

object ProjectController {
  def excToStatusCode(e: ProjectServiceException): StatusCode = e match {
    case _: ProjectService.Exceptions.NotFound      => StatusCodes.NotFound
    case _: ProjectService.Exceptions.AccessDenied  => StatusCodes.Forbidden
    case _: ProjectService.Exceptions.InternalError => StatusCodes.InternalServerError
  }

  val projectServiceExceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: ProjectServiceException => complete(excToStatusCode(e), e.getMessage)
    case e                          => complete(StatusCodes.InternalServerError, e.getMessage)
  }
}
