package cromwell.pipeline.service.impls

import cromwell.pipeline.datastorage.dao.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.service.ProjectService

import scala.concurrent.Future

class ProjectServiceTestImpl(projects: Seq[Project], testMode: TestMode) extends ProjectService {

  def getUserProjectById(projectId: ProjectId, userId: UserId): Future[Project] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(projects.head)
    }

  def getUserProjectByName(namePattern: String, userId: UserId): Future[Project] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(projects.head)
    }

  def addProject(request: ProjectAdditionRequest, userId: UserId): Future[Project] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(TestProjectUtils.getDummyProject(name = request.name, ownerId = userId))
    }

  def deactivateProjectById(projectId: ProjectId, userId: UserId): Future[Project] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(projects.head)
    }

  def updateProjectName(projectId: ProjectId, request: ProjectUpdateNameRequest, userId: UserId): Future[ProjectId] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(projectId)
    }

  def updateProjectVersion(projectId: ProjectId, version: PipelineVersion, userId: UserId): Future[Int] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(1)
    }

  override def getUserProjects(userId: UserId): Future[Seq[Project]] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(projects)
    }
}

object ProjectServiceTestImpl {

  def apply(projects: Project*): ProjectServiceTestImpl =
    new ProjectServiceTestImpl(projects = projects, testMode = Success)

  def withException(exception: Throwable): ProjectServiceTestImpl =
    new ProjectServiceTestImpl(projects = Seq.empty, testMode = WithException(exception))

}
