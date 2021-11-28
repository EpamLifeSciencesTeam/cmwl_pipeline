package cromwell.pipeline.service.impls

import cromwell.pipeline.datastorage.dto.{ Project, ProjectSearchQuery }
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.service.ProjectSearchEngine

import scala.concurrent.Future

class ProjectSearchEngineTestImpl(projects: Seq[Project], testMode: TestMode) extends ProjectSearchEngine {

  override def searchProjects(query: ProjectSearchQuery, userId: UserId): Future[Seq[Project]] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(projects)
    }
}

object ProjectSearchEngineTestImpl {

  def apply(projects: Project*): ProjectSearchEngineTestImpl =
    new ProjectSearchEngineTestImpl(projects = projects, testMode = Success)

  def withException(exception: Throwable): ProjectSearchEngineTestImpl =
    new ProjectSearchEngineTestImpl(projects = Seq.empty, testMode = WithException(exception))

}
