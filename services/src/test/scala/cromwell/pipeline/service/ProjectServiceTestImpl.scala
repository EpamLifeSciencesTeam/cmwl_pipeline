package cromwell.pipeline.service
import cromwell.pipeline.datastorage.dao.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.service.ProjectService.Exceptions.{ ProjectAccessDeniedException, ProjectNotFoundException }

import scala.collection.mutable
import scala.concurrent.{ ExecutionContext, Future }

class ProjectServiceTestImpl(implicit executionContext: ExecutionContext) extends ProjectService {

  val projects: mutable.Map[ProjectId, Project] = mutable.Map.empty

  private def getForUserOrFail(projects: Seq[Project], userId: UserId): Future[Project] =
    projects.find(_.ownerId == userId) match {
      case Some(project)             => Future.successful(project)
      case None if projects.nonEmpty => Future.failed(new ProjectAccessDeniedException)
      case _                         => Future.failed(new ProjectNotFoundException)
    }

  override private[service] def getUserProjectById(projectId: ProjectId, userId: UserId): Future[Project] =
    getForUserOrFail(projects.get(projectId).toSeq, userId)

  override def getUserProjectByName(namePattern: String, userId: UserId): Future[Project] =
    getForUserOrFail(projects.values.toSeq.filter(_.name == namePattern), userId)

  override def addProject(
    request: ProjectAdditionRequest,
    userId: UserId
  ): Future[Either[VersioningException, Project]] = {
    val dummyProject = TestProjectUtils.getDummyProject(name = request.name, ownerId = userId)
    projects += (dummyProject.projectId -> dummyProject)

    Future.successful(Right(dummyProject))
  }

  override def deactivateProjectById(projectId: ProjectId, userId: UserId): Future[Project] =
    getForUserOrFail(projects.get(projectId).toSeq, userId).flatMap { project =>
      val deactivatedProject = project.copy(active = false)
      projects += (projectId -> deactivatedProject)

      Future.successful(deactivatedProject)
    }

  override def updateProjectName(request: ProjectUpdateNameRequest, userId: UserId): Future[ProjectId] =
    getForUserOrFail(projects.get(request.projectId).toSeq, userId).flatMap { project =>
      val updatedNameProject = project.copy(name = request.name)
      projects += (request.projectId -> updatedNameProject)

      Future.successful(updatedNameProject.projectId)
    }

  override def updateProjectVersion(projectId: ProjectId, version: PipelineVersion, userId: UserId): Future[Int] =
    getForUserOrFail(projects.get(projectId).toSeq, userId).flatMap { project =>
      val updatedVersionProject = project.copy(version = version)
      projects += (projectId -> updatedVersionProject)

      Future.successful(0)
    }
}

object ProjectServiceTestImpl {

  def apply(testProjects: Project*)(implicit executionContext: ExecutionContext): ProjectService = {
    val projectServiceTestImpl = new ProjectServiceTestImpl
    testProjects.foreach(project => projectServiceTestImpl.projects += (project.projectId -> project))

    projectServiceTestImpl
  }

}
