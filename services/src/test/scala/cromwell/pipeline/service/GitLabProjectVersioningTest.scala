package cromwell.pipeline.service
import cromwell.pipeline.datastorage.dao.repository.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto.Project
import org.mockito.Mockito.when
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.{ ExecutionContext, Future }

class GitLabProjectVersioningTest extends WordSpec with Matchers with MockitoSugar with BeforeAndAfterAll {
  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  val mockHttpClient: HttpClient = mock[HttpClient]
  val gitLabProjectVersioning: GitLabProjectVersioning = new GitLabProjectVersioning(mockHttpClient)
  import ProjectContext._
  import GitLabConfig._
  "GitLabProjectVersioning" when {
    "createRepository" should {
      "throw new VersioningException for inactive project" taggedAs Service in {
        gitLabProjectVersioning.createRepository(inactiveProject).map {
          _ shouldBe Future(throw VersioningException("Could not create a repository for not an active project."))
        }
      }
      "return new active Project with 201 response" taggedAs Service in {
        val project = activeProject.copy(repository = PATH + activeProject.projectId.value)
        def params(project: Project) =
          Map(("name", project.ownerId.value), ("path", project.projectId.value), ("visibility", "private"))
        when(
          mockHttpClient.post(URL + "projects", params(activeProject), TOKEN, "")
        ).thenReturn(Future.successful(Response(201, "", Map())))
        gitLabProjectVersioning.createRepository(activeProject).map {
          _ shouldBe Future(Right(project))
        }
      }
      "throw new VersioningException with not 404 response" taggedAs Service in {
        def params(project: Project) =
          Map(("name", project.ownerId.value), ("path", project.projectId.value), ("visibility", "private"))
        when(mockHttpClient.post(URL + "projects", params(activeProject), TOKEN, ""))
          .thenReturn(Future.successful(Response(404, "", Map())))
        gitLabProjectVersioning.createRepository(activeProject).map {
          _ shouldBe Future(
            Left(throw VersioningException("The repository was not created. Response status: 404"))
          )
        }
      }
    }
  }

  object ProjectContext {
    lazy val activeProject: Project = TestProjectUtils.getDummyProject()
    lazy val inactiveProject: Project = activeProject.copy(active = false);
  }
}
