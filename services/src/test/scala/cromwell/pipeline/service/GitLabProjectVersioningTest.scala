package cromwell.pipeline.service
import cromwell.pipeline.datastorage.dao.repository.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto.{ Project, Version }
import cromwell.pipeline.utils.{ ApplicationConfig, GitLabConfig, HttpStatusCodes }
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
        val project = withRepoProject
        when(request(activeProject)).thenReturn(Future.successful(Response(HttpStatusCodes.Created, EmptyBody, Map())))
        gitLabProjectVersioning.createRepository(activeProject).map {
          _ shouldBe Right(project)
        }
      }
      "throw new VersioningException with 400 response" taggedAs Service in {
        when(request(activeProject))
          .thenReturn(Future.successful(Response(HttpStatusCodes.BadRequest, EmptyBody, Map())))
        gitLabProjectVersioning.createRepository(activeProject).map {
          _ shouldBe Left(VersioningException("The repository was not created. Response status: 400"))
        }
      }
    }

    "getProjectVersions" should {
      val dummyVersionsJson: String = s"[${Json.stringify(Json.toJson(dummyVersion))}]"
      def request(project: Project) =
        mockHttpClient.get(
          url = gitLabConfig.url + "projects/" + project.repository.get.value + "/repository/tags",
          headers = gitLabConfig.token
        )
      "return list of Project versions with 200 response" taggedAs Service in {
        when(request(withRepoProject))
          .thenReturn(Future.successful(Response(HttpStatusCodes.OK, dummyVersionsJson, Map())))
        gitLabProjectVersioning.getProjectVersions(withRepoProject).map {
          _ shouldBe Right(Seq(dummyVersion))
        }
      }
      "throw new VersioningException with 400 response" taggedAs Service in {
        when(request(withRepoProject))
          .thenReturn(Future.successful(Response(HttpStatusCodes.BadRequest, EmptyBody, Map())))
        gitLabProjectVersioning.getProjectVersions(withRepoProject).map {
          _ shouldBe Left(VersioningException("Could not take versions. Response status: 400"))
        }
      }
    }
  }

  object ProjectContext {
    val EmptyBody: String = ""
    lazy val activeProject: Project = TestProjectUtils.getDummyProject()
    lazy val inactiveProject: Project = activeProject.copy(active = false)
    lazy val noRepoProject: Project = activeProject.copy(repository = None)
    lazy val withRepoProject: Project =
      activeProject.withRepository(Some(s"${gitLabConfig.idPath}${activeProject.projectId.value}"))
    lazy val dummyVersion: Version = TestProjectUtils.getDummyVersion()
  }
}
