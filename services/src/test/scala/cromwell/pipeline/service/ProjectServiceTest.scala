package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.ProjectRepository
import cromwell.pipeline.datastorage.dao.repository.utils.TestProjectUtils
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
      "return id of a new project" taggedAs Service in {
        val request = ProjectAdditionRequest(name = dummyProject.name)
        val projectId = dummyProject.projectId
        val ownerId = dummyProject.ownerId

        when(projectVersioning.createRepository(any[LocalProject])(any[ExecutionContext]))
          .thenReturn(Future.successful(Right(dummyProject)))
        when(projectRepository.addProject(any[Project])).thenReturn(Future.successful(projectId))

        projectService.addProject(request, ownerId).map { _ shouldBe Right(projectId) }
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
      "return correct version of project" taggedAs Service in {
        val projectId = ProjectId("projectId")
        val userId = UserId.random
        val version = PipelineVersion("v1.0.0")

        when(projectRepository.getProjectById(projectId)).thenReturn(Future(Some(dummyProject.copy(version = version))))

        projectService.updateProjectVersion(version, projectId, userId)
        projectRepository.getProjectById(projectId).map {
          case Some(project) => project.version shouldBe version
          case None          => fail()
        }
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
