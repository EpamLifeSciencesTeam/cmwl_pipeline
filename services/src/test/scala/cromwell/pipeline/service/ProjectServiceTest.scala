package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.ProjectRepository
import cromwell.pipeline.datastorage.dto.{Project, ProjectAdditionRequest, ProjectId, UserId}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.{AsyncWordSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class ProjectServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {

  private val projectRepository = mock[ProjectRepository]
  private val projectService = new ProjectService(projectRepository)

  "ProjectServiceTest" when {

    "addProject" should {
      "return id of a new project" taggedAs (Service) in {
        val request =
          ProjectAdditionRequest(
            name = "projectName"
          )
        val projectId = TestProjectUtils.getDummyProject().projectId
        val ownerId = TestProjectUtils.getDummyProject().ownerId

        when(projectRepository.addProject(any[Project])).thenReturn(Future(projectId))

        projectService.addProject(request, ownerId).map { _ shouldBe projectId }
      }
    }

    "deactivateProjectById" should {
      "return deactivated project" in {

        val projectId = ProjectId("projectId")
        val project =
          Project(
            projectId = projectId,
            ownerId = UserId("userId"),
            name = "projectName",
            repository = "repositoryName",
            active = false
          )

        when(projectRepository.deactivateProjectById(projectId)).thenReturn(Future(0))
        when(projectRepository.getProjectById(projectId)).thenReturn(Future(Some(project)))

        projectService.deactivateProjectById(projectId).map { _ shouldBe Some(project) }
      }
    }

    "getProjectById" should {
      "return project with corresponding id" in {
        val projectId = ProjectId("projectId")
        val project =
          Project(
            projectId = projectId,
            ownerId = UserId("userId"),
            name = "projectName",
            repository = "repositoryName",
            active = false
          )

        when(projectRepository.getProjectById(projectId)).thenReturn(Future(Some(project)))

        projectService.getProjectById(projectId).map { _ shouldBe Some(project) }
      }

      "return none if project not found" in {
        val projectId = ProjectId("projectId")

        when(projectRepository.getProjectById(projectId)).thenReturn(Future(None))

        projectService.getProjectById(projectId).map { _ shouldBe None }
      }
    }
  }
}
