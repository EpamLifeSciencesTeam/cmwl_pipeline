package cromwell.pipeline.service.impls

import cromwell.pipeline.datastorage.dto.{ Project, ProjectSearchRequest, ProjectSearchResponse }
import cromwell.pipeline.model.wrapper.{ ProjectSearchFilterId, UserId }
import cromwell.pipeline.service.ProjectSearchService

import scala.concurrent.Future

class ProjectSearchServiceTestImpl(projects: Seq[Project], testMode: TestMode) extends ProjectSearchService {

  override def searchProjectsByFilterId(
    filterId: ProjectSearchFilterId,
    userId: UserId
  ): Future[ProjectSearchResponse] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(ProjectSearchResponse(filterId, projects))
    }

  override def searchProjectsByNewQuery(request: ProjectSearchRequest, userId: UserId): Future[ProjectSearchResponse] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(ProjectSearchResponse(ProjectSearchFilterId.random, projects))
    }
}

object ProjectSearchServiceTestImpl {

  def apply(projects: Project*): ProjectSearchServiceTestImpl =
    new ProjectSearchServiceTestImpl(projects = projects, testMode = Success)

  def withException(exception: Throwable): ProjectSearchServiceTestImpl =
    new ProjectSearchServiceTestImpl(projects = Seq.empty, testMode = WithException(exception))

}
