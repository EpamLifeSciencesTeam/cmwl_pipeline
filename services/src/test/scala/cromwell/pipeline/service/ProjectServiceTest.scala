package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.ProjectRepository
import cromwell.pipeline.datastorage.dao.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.service.ProjectService.Exceptions._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class ProjectServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {

  private val projectRepository = mock[ProjectRepository]
  private val projectVersioning = mock[ProjectVersioning[VersioningException]]
  private val projectService = ProjectService(projectRepository, projectVersioning)
  private val dummyProject: Project = TestProjectUtils.getDummyProject()

  "ProjectServiceTest" when {

    "getUserProjects" should {
      val ownerId = dummyProject.ownerId

      "return list projects" taggedAs Service in {
        when(projectRepository.getProjectsByOwnerId(ownerId)).thenReturn(Future.successful(Seq(dummyProject)))

        projectService.getUserProjects(ownerId).map {
          _ shouldBe Seq(dummyProject)
        }
      }

      "return empty list projects" taggedAs Service in {
        when(projectRepository.getProjectsByOwnerId(ownerId)).thenReturn(Future.successful(Seq()))

        projectService.getUserProjects(ownerId).map {
          _ shouldBe Seq.empty
        }
      }
    }

    "addProject" should {
      val request = ProjectAdditionRequest(name = dummyProject.name)
      val ownerId = dummyProject.ownerId

      "return a new project" taggedAs Service in {
        when(projectVersioning.createRepository(any[LocalProject])).thenReturn(Future.successful(Right(dummyProject)))
        when(projectRepository.addProject(dummyProject)).thenReturn(Future.successful(dummyProject.projectId))

        projectService.addProject(request, ownerId).map {
          _ shouldBe dummyProject
        }
      }

      "fail with InternalError" taggedAs Service in {
        val repositoryException = VersioningException.RepositoryException("VersioningException")
        when(projectVersioning.createRepository(any[LocalProject]))
          .thenReturn(Future.successful(Left(repositoryException)))
        projectService.addProject(request, ownerId).failed.map {
          _ shouldBe InternalError("Failed to create project due to unexpected internal error")
        }
      }
    }

    "updateProjectName" should {
      "return projectId if update was successful" taggedAs Service in {
        val newProjectName = s"new${dummyProject.name}"
        val projectId = dummyProject.projectId
        val ownerId = dummyProject.ownerId
        val request = ProjectUpdateNameRequest(newProjectName)

        when(projectRepository.getProjectById(projectId)).thenReturn(Future.successful(Some(dummyProject)))
        when(projectRepository.updateProjectName(dummyProject.copy(name = newProjectName)))
          .thenReturn(Future.successful(1))

        projectService.updateProjectName(projectId, request, ownerId).map { _ shouldBe projectId }
      }
    }

    "deactivateProjectById" should {
      "return deactivated project" taggedAs Service in {
        val projectId = ProjectId("projectId")
        val userId = UserId.random
        val project = TestProjectUtils.getDummyProject(projectId, userId)

        when(projectRepository.deactivateProjectById(projectId)).thenReturn(Future(0))
        when(projectRepository.getProjectById(projectId)).thenReturn(Future(Some(project)))

        projectService.deactivateProjectById(projectId, userId).map { _ shouldBe project }
      }
    }

    "updateProjectVersion" should {
      "succeed if repository returned successful result" taggedAs Service in {
        val projectId = ProjectId("projectId")
        val userId = UserId.random
        val version = PipelineVersion("v1.0.0")
        val project = TestProjectUtils.getDummyProject(projectId, userId)

        when(projectRepository.getProjectById(projectId)).thenReturn(Future.successful(Some(project)))
        when(projectRepository.updateProjectVersion(project.copy(version = version))).thenReturn(Future.successful(0))

        projectService.updateProjectVersion(projectId, version, userId).map(_ shouldBe 0)
      }
    }

    "getUserProjectById" should {
      val userId = UserId.random
      "return project with corresponding id" taggedAs Service in {
        val projectId = ProjectId("projectId")
        val project = TestProjectUtils.getDummyProject(projectId, userId)

        when(projectRepository.getProjectById(projectId)).thenReturn(Future(Some(project)))

        projectService.getUserProjectById(projectId, userId).map { _ shouldBe project }
      }

      "return none if project not found" taggedAs Service in {
        val projectId = ProjectId("projectId")

        when(projectRepository.getProjectById(projectId)).thenReturn(Future(None))

        projectService.getUserProjectById(projectId, userId).failed.map { _ shouldBe NotFound() }
      }
    }

    "getUserProjectByName" should {
      val project1 = TestProjectUtils.getDummyProject()
      val project2 = TestProjectUtils.getDummyProject(name = project1.name)

      "return project with corresponding user id" taggedAs Service in {
        when(projectRepository.getProjectsByName(project2.name)).thenReturn(Future.successful(Seq(project1, project2)))
        projectService.getUserProjectByName(project2.name, project2.ownerId).map { _ shouldBe project2 }
      }

      "fail with ProjectAccessDeniedException" taggedAs Service in {
        when(projectRepository.getProjectsByName(project1.name)).thenReturn(Future.successful(Seq(project2)))
        projectService.getUserProjectByName(project1.name, project1.ownerId).failed.map {
          _ shouldBe AccessDenied()
        }
      }

      "fail with ProjectNotFoundException" taggedAs Service in {
        when(projectRepository.getProjectsByName(project1.name)).thenReturn(Future.successful(Seq()))
        projectService.getUserProjectByName(project1.name, project1.ownerId).failed.map {
          _ shouldBe NotFound()
        }
      }
    }
  }
}
