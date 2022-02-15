package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.impls.ProjectRepositoryTestImpl
import cromwell.pipeline.datastorage.dao.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.ProjectId
import cromwell.pipeline.service.ProjectService.Exceptions.{ AccessDenied, InternalError, NotFound }
import cromwell.pipeline.service.impls.ProjectVersioningTestImpl
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

class ProjectServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {

  private val dummyProject: Project = TestProjectUtils.getDummyProject()

  private def defaultProjectRepository = ProjectRepositoryTestImpl(dummyProject)
  private val defaultProjectVersioning = ProjectVersioningTestImpl(projects = List(dummyProject))

  private def createProjectService(
    projectRepository: ProjectRepositoryTestImpl = defaultProjectRepository,
    projectVersioning: ProjectVersioningTestImpl = defaultProjectVersioning
  ): ProjectService =
    ProjectService(projectRepository, projectVersioning)

  def projectService: ProjectService = createProjectService()

  "ProjectServiceTest" when {

    "getUserProjects" should {
      val ownerId = dummyProject.ownerId

      "return list projects" taggedAs Service in {

        projectService.getUserProjects(ownerId).map {
          _ shouldBe Seq(dummyProject)
        }
      }

      "return empty list projects" taggedAs Service in {
        val emptyProjectRepository = ProjectRepositoryTestImpl()
        val projectService = createProjectService(projectRepository = emptyProjectRepository)

        projectService.getUserProjects(ownerId).map {
          _ shouldBe Seq.empty
        }
      }
    }

    "addProject" should {
      val request = ProjectAdditionRequest(name = dummyProject.name)
      val ownerId = dummyProject.ownerId

      "return a new project" taggedAs Service in {

        projectService.addProject(request, ownerId).map {
          _ shouldBe dummyProject
        }
      }

      "fail with InternalError" taggedAs Service in {
        val repositoryException = VersioningException.RepositoryException("VersioningException")
        val projectService =
          createProjectService(projectVersioning = ProjectVersioningTestImpl.withException(repositoryException))
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

        projectService.updateProjectName(projectId, request, ownerId).map { _ shouldBe projectId }
      }
    }

    "deactivateProjectById" should {
      "return deactivated project" taggedAs Service in {
        val projectId = dummyProject.projectId
        val userId = dummyProject.ownerId
        val deactivatedProject = dummyProject.copy(active = false)

        projectService.deactivateProjectById(projectId, userId).map { _ shouldBe deactivatedProject }
      }
    }

    "updateProjectVersion" should {
      "succeed if repository returned successful result" taggedAs Service in {
        val projectId = dummyProject.projectId
        val userId = dummyProject.ownerId
        val newVersion = TestProjectUtils.getDummyPipeLineVersion()

        projectService.updateProjectVersion(projectId, newVersion, userId).map(_ shouldBe 0)
      }
    }

    "getUserProjectById" should {
      val userId = dummyProject.ownerId
      "return project with corresponding id" taggedAs Service in {
        val projectId = dummyProject.projectId

        projectService.getUserProjectById(projectId, userId).map { _ shouldBe dummyProject }
      }

      "return none if project not found" taggedAs Service in {
        val projectId = ProjectId.random

        projectService.getUserProjectById(projectId, userId).failed.map { _ shouldBe NotFound() }
      }
    }

    "getUserProjectByName" should {
      val project1 = dummyProject
      val project2 = TestProjectUtils.getDummyProject(name = dummyProject.name)

      "return project with corresponding user id" taggedAs Service in {
        val projectService = createProjectService(projectRepository = ProjectRepositoryTestImpl(project1, project2))

        projectService.getUserProjectByName(project2.name, project2.ownerId).map { _ shouldBe project2 }
      }

      "fail with ProjectAccessDeniedException" taggedAs Service in {
        val projectService = createProjectService(projectRepository = ProjectRepositoryTestImpl(project2))

        projectService.getUserProjectByName(project1.name, project1.ownerId).failed.map {
          _ shouldBe AccessDenied()
        }
      }

      "fail with ProjectNotFoundException" taggedAs Service in {
        val projectService = createProjectService(projectRepository = ProjectRepositoryTestImpl())

        projectService.getUserProjectByName(project1.name, project1.ownerId).failed.map {
          _ shouldBe NotFound()
        }
      }
    }
  }
}
