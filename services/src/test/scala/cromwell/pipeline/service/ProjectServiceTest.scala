package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.ProjectRepository
import cromwell.pipeline.datastorage.dao.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto.{ LocalProject, PipelineVersion, Project, ProjectAdditionRequest, ProjectId }
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.service.Exceptions.ProjectNotFoundException
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.{ ExecutionContext, Future }

class ProjectServiceTest extends AsyncWordSpec with Matchers with MockitoSugar with BeforeAndAfterAll {

  private val projectRepository = mock[ProjectRepository]
  private val projectVersioning = mock[ProjectVersioning[VersioningException]]
  private val projectService = new ProjectService(projectRepository, projectVersioning)
  private val dummyProject: Project = TestProjectUtils.getDummyProject()

  "ProjectServiceTest" when {

    "addProject" should {
      val request = ProjectAdditionRequest(name = dummyProject.name)
      val ownerId = dummyProject.ownerId

      "return a new project" taggedAs Service in {
        when(projectVersioning.createRepository(any[LocalProject])(any[ExecutionContext]))
          .thenReturn(Future.successful(Right(dummyProject)))
        when(projectRepository.addProject(dummyProject)).thenReturn(Future.successful(dummyProject.projectId))

        projectService.addProject(request, ownerId).map {
          _ shouldBe Right(dummyProject)
        }
      }

      "fail with VersioningException.RepositoryException" taggedAs Service in {
        val repositoryException = VersioningException.RepositoryException("VersioningException")
        when(projectVersioning.createRepository(any[LocalProject])(any[ExecutionContext]))
          .thenReturn(Future.successful(Left(repositoryException)))
        projectService.addProject(request, ownerId).map {
          _ shouldBe Left(repositoryException)
        }
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

        projectService.getUserProjectById(projectId, userId).failed.map { _ shouldBe ProjectNotFoundException() }
      }
    }
  }
}
