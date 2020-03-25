package cromwell.pipeline.service

import java.net.URLEncoder
import java.nio.file.Paths

import akka.http.scaladsl.model.StatusCodes
import cromwell.pipeline.datastorage.dao.repository.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto.File.UpdateFileRequest
import cromwell.pipeline.datastorage.dto._
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
  import ProjectFileContext._

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

    "updateFile" should {
      "return create file response when file is new" taggedAs Service in {
        val path = URLEncoder.encode(newFile.path.toString, "UTF-8")
        val url = s"${gitLabConfig.url}projects/${withRepoProject.repository}/repository/files/$path"

        when(
          mockHttpClient.put(
            url,
            payload = Json.stringify(
              Json.toJson(
                File
                  .UpdateFileRequest(gitLabConfig.defaultFileVersion, newFile.content, gitLabConfig.defaultFileVersion)
              )
            ),
            headers = gitLabConfig.token
          )
        ).thenReturn(
          Future.successful(
            Response(StatusCodes.BadRequest.intValue, "File does not exist", Map("error" -> List("true")))
          )
        )
        when(
          mockHttpClient.post(
            url,
            payload = Json.stringify(
              Json.toJson(File.UpdateFileRequest(gitLabConfig.defaultFileVersion, newFile.content, "Init commit"))
            ),
            headers = gitLabConfig.token
          )
        ).thenReturn(
          Future.successful(Response(StatusCodes.OK.intValue, "Create file", Map("success" -> List("true"))))
        )

        gitLabProjectVersioning.updateFile(withRepoProject, newFile, None).flatMap(_ shouldBe Right("Create new file"))
      }

      "return update file response when file is already exist" taggedAs Service in {
        val path = URLEncoder.encode(existFile.path.toString, "UTF-8")
        val url = s"${gitLabConfig.url}projects/${withRepoProject.repository}/repository/files/$path"

        when(
          mockHttpClient.put(
            url,
            payload = Json.stringify(Json.toJson(UpdateFileRequest("v.0.0.2", existFile.content, "v.0.0.2"))),
            headers = gitLabConfig.token
          )
        ).thenReturn(
          Future.successful(
            Response(StatusCodes.OK.intValue, "File update", Map("success" -> List("true")))
          )
        )

        gitLabProjectVersioning
          .updateFile(
            withRepoProject,
            existFile,
            Some(Version("v.0.0.2", "New version", "this project", Commit("commit_12")))
          )
          .flatMap(_ shouldBe Right("Success update file"))
      }
    }

    "getFile" should {
      "return file with 200 response" taggedAs Service in {
        val project = TestProjectUtils.getDummyProject()
        val path = Paths.get("test.md")
        val version = dummyVersion;

        when(
          mockHttpClient.get(
            s"${gitLabConfig.url}/projects/${project.repository}/repository/files/${URLEncoder.encode(path.toString, "UTF-8")}/raw",
            Map("ref" -> version.name),
            gitLabConfig.token
          )
        ).thenReturn(Future.successful(Response(200, "Test File", Map())))

        gitLabProjectVersioning
          .getFile(project, path, Some(version))
          .map(_ shouldBe (Right(ProjectFile(path, "Test File"))))
      }

      "throw new VersioningException with not 200 response" taggedAs Service in {
        val project = TestProjectUtils.getDummyProject()
        val path = Paths.get("test.md")
        val version = dummyVersion;

        when(
          mockHttpClient.get(
            s"${gitLabConfig.url}/projects/${project.repository}/repository/files/${URLEncoder.encode(path.toString, "UTF-8")}/raw",
            Map("ref" -> version.name),
            gitLabConfig.token
          )
        ).thenReturn(Future.successful(Response(404, "Not Found", Map())))

        gitLabProjectVersioning
          .getFile(project, path, Some(version))
          .map(_ shouldBe Left(VersioningException("Exception. Response status: 404")))
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

  object ProjectFileContext {
    val newFile: ProjectFile = ProjectFile(Paths.get("new_file.txt"), "Hello world")
    val existFile: ProjectFile = ProjectFile(Paths.get("exist_file.txt"), "Hello world")
  }

}
