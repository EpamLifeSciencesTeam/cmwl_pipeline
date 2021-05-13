package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.datastorage.dto.{ ProjectAdditionRequest, ProjectDeleteRequest, ProjectUpdateNameRequest }
import cromwell.pipeline.service.Exceptions.{ ProjectAccessDeniedException, ProjectNotFoundException }
import cromwell.pipeline.service.{ ProjectService, VersioningException }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.util.{ Failure, Success }

class ProjectController(projectService: ProjectService) {

  private def getProject(implicit accessToken: AccessTokenContent): Route = get {
    parameter('name.as[String]) { name =>
      onComplete(projectService.getUserProjectByName(name, accessToken.userId)) {
        case Success(project)                         => complete(project)
        case Failure(e: ProjectNotFoundException)     => complete(StatusCodes.NotFound, e.getMessage)
        case Failure(e: ProjectAccessDeniedException) => complete(StatusCodes.Forbidden, e.getMessage)
        case Failure(e)                               => complete(StatusCodes.InternalServerError, e.getMessage)
      }
    }
  }

  private def addProject(implicit accessToken: AccessTokenContent): Route = post {
    entity(as[ProjectAdditionRequest]) { request =>
      onComplete(projectService.addProject(request, accessToken.userId)) {
        case Success(project) =>
          project match {
            case Right(project)               => complete(project)
            case Left(e: VersioningException) => complete(StatusCodes.InternalServerError, e.getMessage)
          }
        case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
      }
    }
  }

  private def deactivateProject(implicit accessToken: AccessTokenContent): Route = delete {
    entity(as[ProjectDeleteRequest]) { request =>
      onComplete(projectService.deactivateProjectById(request.projectId, accessToken.userId)) {
        case Success(project)                         => complete(project)
        case Failure(e: ProjectNotFoundException)     => complete(StatusCodes.NotFound, e.getMessage)
        case Failure(e: ProjectAccessDeniedException) => complete(StatusCodes.Forbidden, e.getMessage)
        case Failure(e)                               => complete(StatusCodes.InternalServerError, e.getMessage)
      }
    }
  }

  private def updateProject(implicit accessToken: AccessTokenContent): Route = put {
    entity(as[ProjectUpdateNameRequest]) { request =>
      onComplete(projectService.updateProjectName(request, accessToken.userId)) {
        case Success(_) => complete(StatusCodes.NoContent)
        case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
      }
    }
  }

  val route: AccessTokenContent => Route = implicit accessToken =>
    pathPrefix("projects") {
      getProject ~
      addProject ~
      deactivateProject ~
      updateProject
    }
}
