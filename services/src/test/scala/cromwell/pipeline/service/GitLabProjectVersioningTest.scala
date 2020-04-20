package cromwell.pipeline.service
import akka.http.scaladsl.model.StatusCodes
import cromwell.pipeline.datastorage.dao.repository.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto.Project
import cromwell.pipeline.utils.{ ApplicationConfig, GitLabConfig }
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json

import scala.concurrent.Future

class GitLabProjectVersioningTest extends AsyncWordSpec with ScalaFutures with Matchers with MockitoSugar {
  val mockHttpClient: HttpClient = mock[HttpClient]
  val gitLabConfig: GitLabConfig = ApplicationConfig.load().gitLabConfig
  val gitLabProjectVersioning: GitLabProjectVersioning = new GitLabProjectVersioning(mockHttpClient, gitLabConfig)
  import ProjectContext._
  "GitLabProjectVersioning" when {
    "createRepository" should {
      def payload(project: Project): String = Json.stringify(Json.toJson(project))
      def request(project: Project) =
        mockHttpClient.post(
          url = gitLabConfig.url + "projects",
          headers = gitLabConfig.token,
          payload = payload(project)
        )

      "throw new VersioningException for inactive project" taggedAs Service in {
        whenReady(gitLabProjectVersioning.createRepository(inactiveProject).failed) {
          _ shouldBe VersioningException("Could not create a repository for deleted project.")
        }
      }
      "return new active Project with 201 response" taggedAs Service in {
        val project = projectWithRepo
        when(request(activeProject)).thenReturn(Future.successful(Response(StatusCodes.Created.intValue, "", Map())))
        gitLabProjectVersioning.createRepository(activeProject).map {
          _ shouldBe Right(project)
        }
      }
      "throw new VersioningException with not 400 response" taggedAs Service in {
        when(request(activeProject)).thenReturn(Future.successful(Response(StatusCodes.BadRequest.intValue, "", Map())))
        gitLabProjectVersioning.createRepository(activeProject).map {
          _ shouldBe Left(VersioningException("The repository was not created. Response status: 400"))
        }
      }
    }
  }

  object ProjectContext {
    lazy val activeProject: Project = TestProjectUtils.getDummyProject()
    lazy val inactiveProject: Project = activeProject.copy(active = false)
    lazy val noRepoProject: Project = activeProject.copy(repository = null)
    lazy val projectWithRepo: Project =
      activeProject.copy(repository = s"${gitLabConfig.idPath}${activeProject.projectId.value}")
  }
}
