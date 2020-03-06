package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.ProjectRepository
import cromwell.pipeline.datastorage.dao.repository.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto.{ Project, ProjectAdditionRequest }
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class ProjectServiceTest extends AsyncWordSpec with Matchers with MockitoSugar with BeforeAndAfterAll {

  private val projectRepository = mock[ProjectRepository]
  private val projectService = new ProjectService(projectRepository)
  private val dummyProject: Project = TestProjectUtils.getDummyProject()

  "ProjectServiceTest" when {

    "addProject" should {
      "return id of a new project" taggedAs Service in {
        val request =
          ProjectAdditionRequest(
            ownerId = dummyProject.ownerId,
            name = dummyProject.name,
            repository = dummyProject.repository
          )

        when(projectRepository.addProject(any[Project])).thenReturn(Future(dummyProject.projectId))

        projectService.addProject(request).map { _ shouldBe dummyProject.projectId }
      }
    }

    "deactivateProjectById" should {
      "return deactivated project" taggedAs Service in {
        val project = dummyProject.copy(active = false)

        when(projectRepository.deactivateProjectById(dummyProject.projectId)).thenReturn(Future(0))
        when(projectRepository.getProjectById(dummyProject.projectId)).thenReturn(Future(Some(project)))

        projectService.deactivateProjectById(dummyProject.projectId).map { _ shouldBe Some(project) }
      }
    }

    "getProjectById" should {
      "return project with corresponding id" taggedAs Service in {
        val project = dummyProject.copy(active = false)

        when(projectRepository.getProjectById(dummyProject.projectId)).thenReturn(Future(Some(project)))

        projectService.getProjectById(dummyProject.projectId).map { _ shouldBe Some(project) }
      }

      "return none if project not found" taggedAs Service in {

        when(projectRepository.getProjectById(dummyProject.projectId)).thenReturn(Future(None))

        projectService.getProjectById(dummyProject.projectId).map { _ shouldBe None }
      }
    }
  }
}
