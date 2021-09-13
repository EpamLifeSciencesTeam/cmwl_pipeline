package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.ProjectRepository
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.service.ProjectService.Exceptions._
import cromwell.pipeline.service.exceptions.ServiceException

import java.util.UUID
import scala.concurrent.{ ExecutionContext, Future }

trait ProjectService {

  def getUserProjectById(projectId: ProjectId, userId: UserId): Future[Project]

  def getUserProjects(userId: UserId): Future[Seq[Project]]

  def getUserProjectByName(namePattern: String, userId: UserId): Future[Project]

  def addProject(request: ProjectAdditionRequest, userId: UserId): Future[Project]

  def deactivateProjectById(projectId: ProjectId, userId: UserId): Future[Project]

  def updateProjectName(projectId: ProjectId, request: ProjectUpdateNameRequest, userId: UserId): Future[ProjectId]

  def updateProjectVersion(projectId: ProjectId, version: PipelineVersion, userId: UserId): Future[Int]

}

object ProjectService {

  object Exceptions {
    sealed abstract class ProjectServiceException(message: String) extends ServiceException(message)

    final case class NotFound(message: String = "Project not found") extends ProjectServiceException(message)
    final case class AccessDenied(message: String = "Access denied. You are not the project owner")
        extends ProjectServiceException(message)
    final case class InternalError(message: String = "Internal error") extends ProjectServiceException(message)
  }

  def apply(projectRepository: ProjectRepository, projectVersioning: ProjectVersioning[VersioningException])(
    implicit executionContext: ExecutionContext
  ): ProjectService =
    new ProjectService {

      def getUserProjectById(projectId: ProjectId, userId: UserId): Future[Project] =
        projectRepository
          .getProjectById(projectId)
          .recoverWith {
            case _ => internalError("find user")
          }
          .flatMap(project => getForUserOrFail(project.toSeq, userId))

      def getUserProjects(userId: UserId): Future[Seq[Project]] =
        projectRepository.getProjectsByOwnerId(userId).recoverWith {
          case _ => internalError("find user")
        }

      def getUserProjectByName(namePattern: String, userId: UserId): Future[Project] =
        projectRepository
          .getProjectsByName(namePattern)
          .recoverWith {
            case _ => internalError("find user")
          }
          .flatMap(getForUserOrFail(_, userId))

      private def getForUserOrFail(projects: Seq[Project], userId: UserId): Future[Project] =
        projects.find(_.ownerId == userId) match {
          case Some(project)             => Future.successful(project)
          case None if projects.nonEmpty => Future.failed(AccessDenied())
          case _                         => Future.failed(NotFound())
        }

      def addProject(request: ProjectAdditionRequest, userId: UserId): Future[Project] = {
        val localProject =
          LocalProject(
            projectId = ProjectId(UUID.randomUUID().toString),
            ownerId = userId,
            name = request.name,
            active = true
          )
        projectVersioning.createRepository(localProject).flatMap {
          case Left(_) => internalError("create project")
          case Right(project) =>
            projectRepository.addProject(project).map(_ => project).recoverWith {
              case _ => internalError("add project")
            }
        }
      }

      def deactivateProjectById(projectId: ProjectId, userId: UserId): Future[Project] =
        getUserProjectById(projectId, userId).flatMap { project =>
          if (!project.active) {
            Future.successful(project)
          } else {
            projectRepository
              .deactivateProjectById(projectId)
              .recoverWith {
                case _ => internalError("deactivate project")
              }
              .flatMap(_ => getUserProjectById(projectId, userId))

          }
        }

      def updateProjectName(
        projectId: ProjectId,
        request: ProjectUpdateNameRequest,
        userId: UserId
      ): Future[ProjectId] =
        getUserProjectById(projectId, userId).flatMap { project =>
          val updatedProject = project.copy(name = request.name)
          projectRepository
            .updateProjectName(updatedProject)
            .recoverWith {
              case _ => internalError("update project")
            }
            .map(_ => updatedProject.projectId)
        }

      def updateProjectVersion(projectId: ProjectId, version: PipelineVersion, userId: UserId): Future[Int] =
        getUserProjectById(projectId, userId).flatMap { project =>
          projectRepository.updateProjectVersion(project.copy(version = version)).recoverWith {
            case _ => internalError("update project")
          }
        }

      private def internalError(action: String): Future[Nothing] =
        Future.failed(InternalError(s"Failed to $action due to unexpected internal error"))
    }
}
