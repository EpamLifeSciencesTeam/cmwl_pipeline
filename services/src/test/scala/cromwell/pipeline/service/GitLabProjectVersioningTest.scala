package cromwell.pipeline.service
import cromwell.pipeline.datastorage.dto.{ Project, ProjectId, UserId, Version }
import org.mockito.Mockito.when
import org.mockito.{ Matchers => MockitoMatchers }
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, Matchers }
import org.scalatestplus.mockito.MockitoSugar
import java.net.URLEncoder
import java.nio.file.Paths
import scala.concurrent.{ ExecutionContext, Future }

class GitLabProjectVersioningTest extends AsyncWordSpec with Matchers with MockitoSugar with BeforeAndAfterAll {

  val mockHttpClient: HttpClient = mock[HttpClient]
  val gitLabProjectVersioning: GitLabProjectVersioning = new GitLabProjectVersioning(mockHttpClient)

  "GitLabProjectVersioning" when {
    "getFile" should {
      "return file with 200 response" taggedAs Service in {
        val project = Project(ProjectId("1"), UserId("test"), "name-1", "repository-1", false)
        val path = Paths.get("test.md")
        val version = Some(Version("master"))

        when(
          mockHttpClient.get(
            MockitoMatchers.eq(
              s"${gitLabProjectVersioning.URL}/projects/${project.projectId.value}/repository/files/${URLEncoder
                .encode(path.toString, "UTF-8")}/raw"
            ),
            MockitoMatchers.eq(Map("ref" -> version.map((el) => el.value).getOrElse("master"))),
            MockitoMatchers.any[Map[String, String]]
          )(MockitoMatchers.any[ExecutionContext])
        ).thenReturn(Future.successful(Response(200, "Test File", Map())))

        gitLabProjectVersioning.getFile(project, path, version).map(_ shouldBe (Right("Test File")))
      }

      "return exception with non 200 response" taggedAs Service in {
        val project = Project(ProjectId("1"), UserId("test"), "name-1", "repository-1", false)
        val path = Paths.get("test.md")
        val version = Some(Version("master"))

        when(
          mockHttpClient.get(
            MockitoMatchers.eq(
              s"${gitLabProjectVersioning.URL}/projects/${project.projectId.value}/repository/files/${URLEncoder
                .encode(path.toString, "UTF-8")}/raw"
            ),
            MockitoMatchers.eq(Map("ref" -> version.map((el) => el.value).getOrElse("master"))),
            MockitoMatchers.any[Map[String, String]]
          )(MockitoMatchers.any[ExecutionContext])
        ).thenReturn(Future.successful(Response(404, "", Map())))

        gitLabProjectVersioning
          .getFile(project, path, version)
          .map(_ shouldBe (Left(VersioningException("Exception. Response status: 404"))))
      }
    }
  }
}
