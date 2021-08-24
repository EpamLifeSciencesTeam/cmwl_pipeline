package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cromwell.pipeline.controller.utils.PathMatchers.ProjectId
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.datastorage.dto.{ ProjectAdditionRequest, ProjectUpdateNameRequest }
import cromwell.pipeline.service.ProjectService.Exceptions.{ ProjectAccessDeniedException, ProjectNotFoundException }
import cromwell.pipeline.service.{ ProjectService, VersioningException }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.util.{ Failure, Success }

class ProjectController(projectService: ProjectService) {

  private def getProjectById(implicit accessToken: AccessTokenContent): Route = get {
    path(ProjectId) { projectId =>
      onComplete(projectService.getUserProjectById(projectId, accessToken.userId)) {
        case Success(project)                         => complete(project)
        case Failure(e: ProjectNotFoundException)     => complete(StatusCodes.NotFound, e.getMessage)
        case Failure(e: ProjectAccessDeniedException) => complete(StatusCodes.Forbidden, e.getMessage)
        case Failure(e)                               => complete(StatusCodes.InternalServerError, e.getMessage)
      }
    }
  }

  private def getProjectByName(implicit accessToken: AccessTokenContent): Route = get {
    pathEndOrSingleSlash {
      parameter('name.as[String]) { name =>
        onComplete(projectService.getUserProjectByName(name, accessToken.userId)) {
          case Success(project)                         => complete(project)
          case Failure(e: ProjectNotFoundException)     => complete(StatusCodes.NotFound, e.getMessage)
          case Failure(e: ProjectAccessDeniedException) => complete(StatusCodes.Forbidden, e.getMessage)
          case Failure(e)                               => complete(StatusCodes.InternalServerError, e.getMessage)
        }
      }
    }
  }

  private def getProjects(implicit accessToken: AccessTokenContent): Route = get {
    pathEndOrSingleSlash {
      onComplete(projectService.getUserProjects(accessToken.userId)) {
        case Success(projects) => complete(projects)
        case Failure(e)        => complete(StatusCodes.InternalServerError, e.getMessage)
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

  private def deactivateProject(implicit accessToken: AccessTokenContent): Route = path(ProjectId) { projectId =>
    delete {
      onComplete(projectService.deactivateProjectById(projectId, accessToken.userId)) {
        case Success(project)                         => complete(project)
        case Failure(e: ProjectNotFoundException)     => complete(StatusCodes.NotFound, e.getMessage)
        case Failure(e: ProjectAccessDeniedException) => complete(StatusCodes.Forbidden, e.getMessage)
        case Failure(e)                               => complete(StatusCodes.InternalServerError, e.getMessage)
      }
    }
  }

  private def updateProject(implicit accessToken: AccessTokenContent): Route = path(ProjectId) { projectId =>
    put {
      entity(as[ProjectUpdateNameRequest]) { request =>
        onComplete(projectService.updateProjectName(projectId, request, accessToken.userId)) {
          case Success(_) => complete(StatusCodes.OK)
          case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
        }
      }
    }
  }

  val route: AccessTokenContent => Route = implicit accessToken =>
    pathPrefix("projects") {
      getProjectById ~
      getProjectByName ~
      getProjects ~
      addProject ~
      deactivateProject ~
      updateProject
    }
}
