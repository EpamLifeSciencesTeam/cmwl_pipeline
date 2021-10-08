package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.service.ProjectConfigurationService.Exceptions.NotFound
import cromwell.pipeline.service.ProjectSearchService.Exceptions._
import cromwell.pipeline.service.exceptions.ServiceException

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

trait ProjectSearchService {

  def searchProjects(request: ProjectSearchRequest, userId: UserId): Future[ProjectSearchResponse]

}

object ProjectSearchService {

  object Exceptions {
    sealed abstract class ProjectSearchException(message: String) extends ServiceException(message)
    final case class InternalError(message: String = "Internal error") extends ProjectSearchException(message)
  }

  // scalastyle:off method.length
  def apply(
    projectService: ProjectService,
    projectFileService: ProjectFileService,
    projectConfigurationService: ProjectConfigurationService
  )(
    implicit ec: ExecutionContext
  ): ProjectSearchService =
    new ProjectSearchService {

      override def searchProjects(request: ProjectSearchRequest, userId: UserId): Future[ProjectSearchResponse] = {

        def doSearch(searchFilter: ProjectSearchFilter, projects: Seq[Project]): Future[Seq[Project]] =
          searchFilter match {
            case All                   => Future.successful(projects)
            case ByName(mode, value)   => Future.successful(searchByNameFilter(mode, value, projects))
            case ByFiles(mode, value)  => searchByFiles(mode, value, projects, userId)
            case ByConfig(mode, value) => searchByConfig(mode, value, projects, userId)

            case Or(leftFilter, rightFilter) =>
              for {
                leftRes <- doSearch(leftFilter, projects)
                rightRes <- doSearch(rightFilter, projects)
              } yield (leftRes ++ rightRes).distinct

            case And(leftFilter, rightFilter) =>
              for {
                leftRes <- doSearch(leftFilter, projects)
                result <- doSearch(rightFilter, leftRes)
              } yield result
          }

        for {
          userProjects <- projectService.getUserProjects(userId).recoverWith {
            case _ => internalError("fetch projects")
          }
          filteredProjects <- doSearch(request.filter, userProjects)
        } yield ProjectSearchResponse(filteredProjects)

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
            case Failure(NotFound(_))   => Future.successful(project, None)
            case _                      => internalError("fetch configuration")
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

      private def searchByNameFilter(
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
