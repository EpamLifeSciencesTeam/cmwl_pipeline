package cromwell.pipeline.service

import java.net.URLEncoder
import java.nio.file.{ Path, Paths }

import akka.http.scaladsl.model.StatusCodes
import cromwell.pipeline.datastorage.dao.repository.utils.TestProjectUtils
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
          _ shouldBe VersioningException.RepositoryException("Could not create a repository for deleted project.")
        }
      }

      "return new active Project with 201 response" taggedAs Service in {
        val project = withRepoProject
        when(request(activeProject))
          .thenReturn(Future.successful(Response(HttpStatusCodes.Created, EmptyBody, EmptyHeaders)))
        gitLabProjectVersioning.createRepository(activeProject).map {
          _ shouldBe Right(project)
        }
      }

      "throw new VersioningException with 400 response" taggedAs Service in {
        when(request(activeProject))
          .thenReturn(Future.successful(Response(HttpStatusCodes.BadRequest, EmptyBody, EmptyHeaders)))
        gitLabProjectVersioning.createRepository(activeProject).map {
          _ shouldBe Left(VersioningException.RepositoryException("The repository was not created. Response status: 400"))
        }
      }
    }

    "getProjectVersions" should {
      val dummyVersionsJson: String = s"[${Json.stringify(Json.toJson(dummyGitLabVersion))}]"

      def request(project: Project) =
        mockHttpClient.get(
          url = gitLabConfig.url + "projects/" + project.repository.get.value + "/repository/tags",
          headers = gitLabConfig.token
        )

      "return list of Project versions with 200 response" taggedAs Service in {
        when(request(withRepoProject))
          .thenReturn(Future.successful(Response(HttpStatusCodes.OK, dummyVersionsJson, EmptyHeaders)))
        gitLabProjectVersioning.getProjectVersions(withRepoProject).map {
          _ shouldBe Right(Seq(dummyGitLabVersion))
        }
      }
      "throw new VersioningException with 400 response" taggedAs Service in {
        when(request(withRepoProject))
          .thenReturn(Future.successful(Response(HttpStatusCodes.BadRequest, EmptyBody, EmptyHeaders)))
        gitLabProjectVersioning.getProjectVersions(withRepoProject).map {
          _ shouldBe Left(VersioningException.ProjectException("Could not take versions. Response status: 400"))
        }
      }
    }
    
    "updateFile" should {
      val successUpdateMessage = "File was updated"
      val successCreateMessage = "File was created"
      val tagUrl = s"${gitLabConfig.url}projects/${withRepoProject.repository.get.value}/repository/tags"
      val gitlabVersion = TestProjectUtils.getDummyGitLabVersion(dummyPipelineVersion)
      when(
        mockHttpClient.post(
          tagUrl,
          params = Map("tag_name" -> dummyPipelineVersionHigher.name, "ref" -> gitLabConfig.defaultBranch),
          headers = gitLabConfig.token,
          payload = EmptyBody
        )
      ).thenReturn(Future.successful(Response(StatusCodes.OK.intValue, "Tag was created", EmptyHeaders)))

      "return create file response when file is new" taggedAs Service in {
        val path = URLEncoder.encode(newFile.path.toString, "UTF-8")
        val url = s"${gitLabConfig.url}projects/${withRepoProject.repository.get.value}/repository/files/$path"
        val payload = Json.stringify(
          Json.toJson(
            File.UpdateFileRequest(newFile.content, dummyPipelineVersionHigher.name, gitLabConfig.defaultBranch)
          )
        )
        when(mockHttpClient.get(tagUrl, headers = gitLabConfig.token)).thenReturn(
          Future.successful(
            Response(StatusCodes.OK.intValue, Json.stringify(Json.toJson(List(gitlabVersion))), EmptyHeaders)
          )
        )
        when(mockHttpClient.put(url, payload = payload, headers = gitLabConfig.token))
          .thenReturn(Future.successful(Response(StatusCodes.BadRequest.intValue, "File does not exist", EmptyHeaders)))
        when(mockHttpClient.post(url, payload = payload, headers = gitLabConfig.token))
          .thenReturn(Future.successful(Response(StatusCodes.OK.intValue, successCreateMessage, EmptyHeaders)))

        gitLabProjectVersioning.updateFile(withRepoProject, newFile, Some(dummyPipelineVersionHigher)).map {
          _ shouldBe Right(successCreateMessage)
        }
      }

      "return update file response when file is already exist" taggedAs Service in {
        val path = URLEncoder.encode(existFile.path.toString, "UTF-8")
        val url = s"${gitLabConfig.url}projects/${withRepoProject.repository.get.value}/repository/files/$path"
        val payload = Json.stringify(
          Json.toJson(
            File.UpdateFileRequest(existFile.content, dummyPipelineVersionHigher.name, gitLabConfig.defaultBranch)
          )
        )
        when(mockHttpClient.get(tagUrl, headers = gitLabConfig.token)).thenReturn(
          Future.successful(
            Response(StatusCodes.OK.intValue, Json.stringify(Json.toJson(List(gitlabVersion))), EmptyHeaders)
          )
        )
        when(mockHttpClient.put(url, payload = payload, headers = gitLabConfig.token))
          .thenReturn(Future.successful(Response(StatusCodes.OK.intValue, successUpdateMessage, EmptyHeaders)))

        gitLabProjectVersioning.updateFile(withRepoProject, existFile, Some(dummyPipelineVersionHigher)).map {
          _ shouldBe Right(successUpdateMessage)
        }
      }
    }

    "getFile" should {
      "return file with 200 response" taggedAs Service in {
        val path = Paths.get("test.md")

        when(
          mockHttpClient.get(
            s"${gitLabConfig.url}/projects/${activeProject.repository}/repository/files/${URLEncoder
              .encode(path.toString, "UTF-8")}/raw",
            Map("ref" -> dummyPipelineVersion.name),
            gitLabConfig.token
          )
        ).thenReturn(Future.successful(Response(200, "Test File", EmptyHeaders)))

        gitLabProjectVersioning
          .getFile(activeProject, path, Some(dummyPipelineVersion))
          .map(_ shouldBe (Right(ProjectFile(path, "Test File"))))
      }

      "throw new VersioningException with not 200 response" taggedAs Service in {
        val path = Paths.get("test.md")

        when(
          mockHttpClient.get(
            s"${gitLabConfig.url}/projects/${activeProject.repository}/repository/files/${URLEncoder
              .encode(path.toString, "UTF-8")}/raw",
            Map("ref" -> dummyPipelineVersion.name),
            gitLabConfig.token
          )
        ).thenReturn(Future.successful(Response(404, "Not Found", EmptyHeaders)))

        gitLabProjectVersioning
          .getFile(activeProject, path, Some(dummyPipelineVersion))
          .map(_ shouldBe Left(VersioningException.HttpException("Exception. Response status: 404")))
      }
    }

    "getFileCommits" should {
      val dummyCommitJson: String = s"${Json.stringify(Json.toJson(List(dummyFileCommit)))}"
      val path: Path = Paths.get("tmp/foo.txt")
      val urlEncoder = URLEncoder.encode(path.toString, "UTF-8")
      def request(project: Project) =
        mockHttpClient.get(
          url = s"${gitLabConfig.url}projects/${project.repository.get.value}/repository/files/${urlEncoder}",
          headers = gitLabConfig.token
        )

      "return list of Project versions with 200 response" taggedAs Service in {
        when(request(withRepoProject))
          .thenReturn(Future.successful(Response(HttpStatusCodes.OK, dummyCommitJson, Map())))
        gitLabProjectVersioning.getFileCommits(withRepoProject, path).map {
          _ shouldBe Right(Seq(dummyFileCommit))
        }
      }

      "throw new VersioningException with 400 response" taggedAs Service in {
        when(request(withRepoProject))
          .thenReturn(Future.successful(Response(HttpStatusCodes.BadRequest, EmptyBody, Map())))
        gitLabProjectVersioning.getFileCommits(withRepoProject, path).map {
          _ shouldBe Left(VersioningException.FileException("Could not take the file commits. Response status: 400"))
        }
      }
    }

    "getFileVersions" should {
      val dummyCommitJson: String = s"${Json.stringify(Json.toJson(List(dummyFileCommit, dummyExistingFileCommit)))}"
      val dummyVersionsJson: String = s"[${Json.stringify(Json.toJson(dummyGitLabVersion))}]"
      val path: Path = Paths.get("tmp/foo.txt")
      val urlEncoder = URLEncoder.encode(path.toString, "UTF-8")
      def projectVersionRequest(project: Project) =
        mockHttpClient.get(
          url = s"${gitLabConfig.url}projects/${project.repository.get.value}/repository/tags",
          headers = gitLabConfig.token
        )

      def fileCommitsRequest(project: Project) =
        mockHttpClient.get(
          url = s"${gitLabConfig.url}projects/${project.repository.get.value}/repository/files/${urlEncoder}",
          headers = gitLabConfig.token
        )

      "return list of Project versions with 200 response" taggedAs Service in {
        when(projectVersionRequest(withRepoProject))
          .thenReturn(Future.successful(Response(HttpStatusCodes.OK, dummyVersionsJson, Map())))
        when(fileCommitsRequest(withRepoProject))
          .thenReturn(Future.successful(Response(HttpStatusCodes.OK, dummyCommitJson, Map())))
        gitLabProjectVersioning.getFileVersions(withRepoProject, path).map {
          _ shouldBe Right(Seq(dummyGitLabVersion))
        }
      }

      "throw new VersioningException" taggedAs Service in {
        when(projectVersionRequest(withRepoProject))
          .thenReturn(Future.successful(Response(HttpStatusCodes.BadRequest, EmptyBody, Map())))
        when(fileCommitsRequest(withRepoProject))
          .thenReturn(Future.successful(Response(HttpStatusCodes.BadRequest, EmptyBody, Map())))
        gitLabProjectVersioning.getFileCommits(withRepoProject, path).map {
          _ shouldBe Left(VersioningException.FileException("Could not take the file commits. Response status: 400"))
        }
      }
    }
  }

  object ProjectContext {
    val EmptyBody: String = ""
    val EmptyHeaders: Map[String, Seq[String]] = Map()
    lazy val activeProject: Project = TestProjectUtils.getDummyProject()
    lazy val inactiveProject: Project = activeProject.copy(active = false)
    lazy val noRepoProject: Project = activeProject.copy(repository = None)
    lazy val withRepoProject: Project =
      activeProject.withRepository(Some(s"${gitLabConfig.idPath}${activeProject.projectId.value}"))
    lazy val dummyPipelineVersion: PipelineVersion = TestProjectUtils.getDummyPipeLineVersion()
    lazy val dummyPipelineVersionHigher: PipelineVersion = dummyPipelineVersion.increaseMinor
    lazy val dummyGitLabVersion: GitLabVersion = TestProjectUtils.getDummyGitLabVersion()
    lazy val dummyFileCommit: FileCommit = TestProjectUtils.getDummyFileCommit()
    lazy val dummyExistingFileCommit: FileCommit = FileCommit(dummyGitLabVersion.commit.id)
  }

  object ProjectFileContext {
    val newFile: ProjectFile = ProjectFile(Paths.get("new_file.txt"), "Hello world")
    val existFile: ProjectFile = ProjectFile(Paths.get("exist_file.txt"), "Hello world")
  }

}