package cromwell.pipeline.service
import cromwell.pipeline.datastorage.dao.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId

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

  def addProject(request: ProjectAdditionRequest, userId: UserId): Future[Either[VersioningException, Project]] =
    testMode match {
      case WithException(exc: VersioningException) => Future.successful(Left(exc))
      case WithException(exc)                      => Future.failed(exc)
      case _ =>
        val newProject = TestProjectUtils.getDummyProject(name = request.name, ownerId = userId)
        Future.successful(Right(newProject))
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

}

object ProjectServiceTestImpl {

  def apply(testProjects: Project*)(implicit testMode: TestMode = Success): ProjectServiceTestImpl =
    new ProjectServiceTestImpl(testProjects, testMode)

}
