package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.service.ProjectSearchEngine.Exceptions._
import cromwell.pipeline.service.exceptions.ServiceException

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

trait ProjectSearchEngine {

  def searchProjects(query: ProjectSearchQuery, userId: UserId): Future[Seq[Project]]

}

object ProjectSearchEngine {

  object Exceptions {
    sealed abstract class ProjectSearchEngineException(message: String) extends ServiceException(message)
    final case class InternalError(message: String = "Internal error") extends ProjectSearchEngineException(message)
  }
  // scalastyle:off method.length
  def apply(
    projectService: ProjectService,
    projectFileService: ProjectFileService,
    projectConfigurationService: ProjectConfigurationService
  )(implicit ec: ExecutionContext): ProjectSearchEngine =
    new ProjectSearchEngine {

      override def searchProjects(query: ProjectSearchQuery, userId: UserId): Future[Seq[Project]] = {

        def doSearch(searchQuery: ProjectSearchQuery, projects: Seq[Project]): Future[Seq[Project]] =
          searchQuery match {
            case All                   => Future.successful(projects)
            case ByName(mode, value)   => Future.successful(searchByName(mode, value, projects))
            case ByFiles(mode, value)  => searchByFiles(mode, value, projects, userId)
            case ByConfig(mode, value) => searchByConfig(mode, value, projects, userId)

            case Or(leftQuery, rightQuery) =>
              for {
                leftRes <- doSearch(leftQuery, projects)
                rightRes <- doSearch(rightQuery, projects)
              } yield (leftRes ++ rightRes).distinct

            case And(leftQuery, rightQuery) =>
              for {
                leftRes <- doSearch(leftQuery, projects)
                result <- doSearch(rightQuery, leftRes)
              } yield result
          }

        for {
          userProjects <- projectService.getUserProjects(userId).recoverWith {
            case _ => internalError("fetch projects")
          }
          filteredProjects <- doSearch(query, userProjects)
        } yield filteredProjects

      }

      private def searchByConfig(
        mode: ContentSearchMode,
        value: Boolean,
        projects: Seq[Project],
        userId: UserId
      ): Future[Seq[Project]] =
        mode match {
          case Exists => searchByConfigExists(value, projects, userId)
        }

      private def searchByConfigExists(
        hasConfig: Boolean,
        projects: Seq[Project],
        userId: UserId
      ): Future[Seq[Project]] = {
        def getProjectWithConfig(project: Project): Future[(Project, Option[ProjectConfiguration])] =
          projectConfigurationService.getLastByProjectId(project.projectId, userId).transformWith {
            case Success(configuration) => Future.successful(project, Some(configuration))
            case Failure(ProjectConfigurationService.Exceptions.NotFound(_)) =>
              Future.successful(project, None)
            case Failure(_) => internalError("fetch configuration")
          }

        for {
          projWithConf <- Future.sequence(projects.map(getProjectWithConfig))
        } yield projWithConf.collect {
          case (proj, Some(_)) if hasConfig => proj
          case (proj, None) if !hasConfig   => proj
        }
      }

      private def searchByFiles(
        mode: ContentSearchMode,
        value: Boolean,
        projects: Seq[Project],
        userId: UserId
      ): Future[Seq[Project]] =
        mode match {
          case Exists => searchByFilesExist(value, projects, userId)
        }

      private def searchByFilesExist(
        hasFiles: Boolean,
        projects: Seq[Project],
        userId: UserId
      ): Future[Seq[Project]] = {
        def getProjectWithFiles(project: Project): Future[(Project, List[ProjectFile])] =
          projectFileService
            .getFiles(project.projectId, None, userId)
            .recoverWith {
              case _ => internalError("fetch files")
            }
            .map(files => (project, files))

        for {
          projWithFiles <- Future.sequence(projects.map(getProjectWithFiles))
        } yield projWithFiles.collect {
          case (proj, list) if list.nonEmpty == hasFiles => proj
        }
      }

      private def searchByName(
        mode: NameSearchMode,
        value: String,
        userProjects: Seq[Project]
      ): Seq[Project] =
        mode match {
          case FullMatch   => userProjects.filter(_.name == value)
          case RegexpMatch => userProjects.filter(_.name.matches(value))
        }

      private def internalError(action: String): Future[Nothing] =
        Future.failed(InternalError(s"Failed to $action due to unexpected internal error"))
    }
  // scalastyle:on method.length
}
