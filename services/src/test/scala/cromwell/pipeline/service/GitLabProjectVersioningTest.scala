package cromwell.pipeline.service

import java.net.URLEncoder
import java.nio.file.{ Path, Paths }

import akka.http.scaladsl.model.StatusCodes
import cromwell.pipeline.datastorage.dao.repository.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto.File.UpdateFileRequest
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.utils.{ AkkaTestSources, ApplicationConfig, GitLabConfig, HttpStatusCodes }
import org.mockito.Mockito.when
import org.mockito.{ Matchers => MockitoMatchers }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Reads

import scala.concurrent.{ ExecutionContext, Future }

class GitLabProjectVersioningTest
    extends AsyncWordSpec
    with ScalaFutures
    with Matchers
    with MockitoSugar
    with AkkaTestSources {

  val mockHttpClient: HttpClient = mock[HttpClient]
  val gitLabConfig: GitLabConfig = ApplicationConfig.load().gitLabConfig
  val gitLabProjectVersioning: GitLabProjectVersioning = new GitLabProjectVersioning(mockHttpClient, gitLabConfig)

  val gitLabErrorText: String =
    """{
      |  "message": {
      |    "path": [
      |    "has already been taken"
      |    ],
      |    "name": [],
      |    "limit_reached": []
      |  }
      |}""".stripMargin

  import ProjectContext._
  import ProjectFileContext._

  "GitLabProjectVersioning" when {

    "createRepository" should {

      def request(postProject: PostProject) =
        mockHttpClient.post[RepositoryId, PostProject](
          url = gitLabConfig.url + "projects",
          headers = gitLabConfig.token,
          payload = postProject
        )

      "throw new VersioningException for inactive project" taggedAs Service in {
        whenReady(gitLabProjectVersioning.createRepository(inactiveProject).failed) {
          _ shouldBe VersioningException.RepositoryException("Could not create a repository for deleted project.")
        }
      }

      "return new active Project with 201 response" taggedAs Service in {

        val project = withGitlabProject

        when(request(postProject)).thenReturn(
          Future.successful(
            Response[RepositoryId](
              HttpStatusCodes.Created,
              SuccessResponseBody[RepositoryId](repositoryId),
              EmptyHeaders
            )
          )
        )

        gitLabProjectVersioning.createRepository(activeProject).map {
          _ shouldBe Right(project)
        }
      }

      "throw new VersioningException with 400 response" taggedAs Service in {
        val errorMsg = "The repository was not created. Response status: 400"
        when(request(postProject)).thenReturn(
          Future.successful(
            Response[RepositoryId](
              HttpStatusCodes.BadRequest,
              FailureResponseBody(errorMsg),
              EmptyHeaders
            )
          )
        )
        gitLabProjectVersioning.createRepository(activeProject).map {
          _ shouldBe Left {
            VersioningException.RepositoryException {
              s"The repository was not created. Response status: 400; Response body [$errorMsg]"
            }
          }
        }
      }
    }

    "getProjectVersions" should {
      def request(project: Project) =
        mockHttpClient.get[Seq[GitLabVersion]](
          url = MockitoMatchers.eq(gitLabConfig.url + "projects/" + project.repository.get.value + "/repository/tags"),
          headers = MockitoMatchers.any[Map[String, String]],
          params = MockitoMatchers.any[Map[String, String]]
        )(MockitoMatchers.any[ExecutionContext], MockitoMatchers.any[Reads[Seq[GitLabVersion]]])

      "return list of Project versions with 200 response" taggedAs Service in {
        when(request(withRepoProject)).thenReturn(
          Future.successful(
            Response[Seq[GitLabVersion]](
              HttpStatusCodes.OK,
              SuccessResponseBody[Seq[GitLabVersion]](dummyVersions),
              EmptyHeaders
            )
          )
        )
        gitLabProjectVersioning.getProjectVersions(withRepoProject).map {
          _ shouldBe Right(dummyVersions)
        }
      }

      "throw new VersioningException with 400 response" taggedAs Service in {
        val activeProject: Project = TestProjectUtils.getDummyProject()
        val withRepoProject: Project =
          activeProject.withRepository(Some(s"${gitLabConfig.idPath}${activeProject.projectId.value}"))
        when(
          request(
            withRepoProject.withRepository(Some(s"${gitLabConfig.idPath}${activeProject.projectId.value}"))
          )
        ).thenReturn(
          Future.successful(
            Response[Seq[GitLabVersion]](
              HttpStatusCodes.BadRequest,
              FailureResponseBody("Response status: 400"),
              EmptyHeaders
            )
          )
        )
        gitLabProjectVersioning.getProjectVersions(withRepoProject).map {
          _ shouldBe Left(
            VersioningException.ProjectException("Could not take versions. ResponseBody: Response status: 400")
          )
        }
      }
    }

    "updateFile" should {
      val successUpdateMessage = "File was updated"
      val successCreateMessage = "File was created"
      val tagUrl = s"${gitLabConfig.url}projects/${withRepoProject.repository.get.value}/repository/tags"
      val gitlabVersion = TestProjectUtils.getDummyGitLabVersion(dummyPipelineVersion)
      when(
        mockHttpClient.post[SuccessResponseMessage, PipelineVersion](
          tagUrl,
          params = Map("tag_name" -> dummyPipelineVersionHigher.name, "ref" -> gitLabConfig.defaultBranch),
          headers = gitLabConfig.token,
          payload = dummyPipelineVersion
        )
      ).thenReturn(
        Future.successful(
          Response[SuccessResponseMessage](
            StatusCodes.OK.intValue,
            SuccessResponseBody(SuccessResponseMessage("Tag was created")),
            EmptyHeaders
          )
        )
      )

      "return create file response when file is new" taggedAs Service in {
        val path = URLEncoder.encode(newFile.path.toString, "UTF-8")
        val url = s"${gitLabConfig.url}projects/${withRepoProject.repository.get.value}/repository/files/$path"
        val payload =
          File.UpdateFileRequest(newFile.content, dummyPipelineVersionHigher.name, gitLabConfig.defaultBranch)

        when(
          mockHttpClient.get[List[GitLabVersion]](
            tagUrl,
            headers = gitLabConfig.token
          )
        ).thenReturn(
          Future.successful(
            Response[List[GitLabVersion]](
              StatusCodes.OK.intValue,
              SuccessResponseBody(List(gitlabVersion)),
              EmptyHeaders
            )
          )
        )
        when(
          mockHttpClient.put[SuccessResponseMessage, UpdateFileRequest](
            url,
            headers = gitLabConfig.token,
            payload = payload
          )
        ).thenReturn(
          Future.successful(
            Response[SuccessResponseMessage](
              StatusCodes.BadRequest.intValue,
              FailureResponseBody("File does not exist"),
              EmptyHeaders
            )
          )
        )
        when(
          mockHttpClient.post[SuccessResponseMessage, UpdateFileRequest](
            url,
            headers = gitLabConfig.token,
            payload = payload
          )
        ).thenReturn(
          Future.successful(
            Response[SuccessResponseMessage](
              StatusCodes.OK.intValue,
              SuccessResponseBody(SuccessResponseMessage(successCreateMessage)),
              EmptyHeaders
            )
          )
        )
        when(
          mockHttpClient.post[SuccessResponseMessage, EmptyPayload](
            tagUrl,
            params = Map("tag_name" -> dummyPipelineVersionHigher.name, "ref" -> gitLabConfig.defaultBranch),
            headers = gitLabConfig.token,
            payload = EmptyPayload()
          )
        ).thenReturn(
          Future.successful(
            Response[SuccessResponseMessage](
              StatusCodes.OK.intValue,
              SuccessResponseBody(SuccessResponseMessage(successCreateMessage)),
              EmptyHeaders
            )
          )
        )

        gitLabProjectVersioning.updateFile(withRepoProject, newFile, Some(dummyPipelineVersionHigher)).map {
          _ shouldBe Right(SuccessResponseMessage(successCreateMessage))
        }
      }

      "return update file response when file is already exist" taggedAs Service in {
        val payload =
          File.UpdateFileRequest(existFile.content, dummyPipelineVersionHigher.name, gitLabConfig.defaultBranch)
        val path = URLEncoder.encode(existFile.path.toString, "UTF-8")
        val url =
          s"${gitLabConfig.url}projects/${withRepoProject.repository.get.value}/repository/files/$path"
        when(mockHttpClient.get[List[GitLabVersion]](tagUrl, headers = gitLabConfig.token)).thenReturn(
          Future.successful(
            Response[List[GitLabVersion]](
              StatusCodes.OK.intValue,
              SuccessResponseBody(List(gitlabVersion)),
              EmptyHeaders
            )
          )
        )
        when(
          mockHttpClient.put[SuccessResponseMessage, UpdateFileRequest](
            url,
            payload = payload,
            headers = gitLabConfig.token
          )
        ).thenReturn(
          Future.successful(
            Response[SuccessResponseMessage](
              StatusCodes.OK.intValue,
              SuccessResponseBody(SuccessResponseMessage(successUpdateMessage)),
              EmptyHeaders
            )
          )
        )
        gitLabProjectVersioning.updateFile(withRepoProject, existFile, Some(dummyPipelineVersionHigher)).map {
          _ shouldBe Right(SuccessResponseMessage(successUpdateMessage))
        }
        assert(true)
      }
    }

    "getFile" should {
      "return file with 200 response" taggedAs Service in {
        val path = Paths.get("test.md")

        when(
          mockHttpClient.get[ProjectFileContent](
            s"${gitLabConfig.url}/projects/${activeProject.repository}/repository/files/${URLEncoder
              .encode(path.toString, "UTF-8")}/raw",
            Map("ref" -> dummyPipelineVersion.name),
            gitLabConfig.token
          )
        ).thenReturn(
          Future.successful(
            Response[ProjectFileContent](200, SuccessResponseBody(ProjectFileContent("Test File")), EmptyHeaders)
          )
        )

        gitLabProjectVersioning
          .getFile(activeProject, path, Some(dummyPipelineVersion))
          .map(_ shouldBe (Right(ProjectFile(path, ProjectFileContent("Test File")))))
      }

      "throw new VersioningException with not 200 response" taggedAs Service in {
        val path = Paths.get("test.md")

        when(
          mockHttpClient.get[ProjectFileContent](
            s"${gitLabConfig.url}/projects/${activeProject.repository}/repository/files/${URLEncoder
              .encode(path.toString, "UTF-8")}/raw",
            Map("ref" -> dummyPipelineVersion.name),
            gitLabConfig.token
          )
        ).thenReturn(
          Future.successful(Response[ProjectFileContent](404, FailureResponseBody("Not Found"), EmptyHeaders))
        )

        gitLabProjectVersioning
          .getFile(activeProject, path, Some(dummyPipelineVersion))
          .map(_ shouldBe Left(VersioningException.HttpException("Exception. Response status: 404")))
      }
    }

    "getFileCommits" should {
      val dummyCommitList = List(dummyFileCommit)
      val path: Path = Paths.get("tmp/foo.txt")
      val urlEncoder = URLEncoder.encode(path.toString, "UTF-8")
      def request(project: Project) =
        mockHttpClient.get[List[FileCommit]](
          url = MockitoMatchers.eq(
            s"${gitLabConfig.url}projects/${project.repository.get.value}/repository/files/${urlEncoder}"
          ),
          params = MockitoMatchers.any[Map[String, String]],
          headers = MockitoMatchers.eq(gitLabConfig.token)
        )(ec = MockitoMatchers.any[ExecutionContext], f = MockitoMatchers.any[Reads[List[FileCommit]]])

      "return list of Project versions with 200 response" taggedAs Service in {
        when(request(withRepoProject)).thenReturn(
          Future.successful(Response[List[FileCommit]](HttpStatusCodes.OK, SuccessResponseBody(dummyCommitList), Map()))
        )
        gitLabProjectVersioning.getFileCommits(withRepoProject, path).map {
          _ shouldBe Right(Seq(dummyFileCommit))
        }
      }

      "throw new VersioningException with 400 response" taggedAs Service in {
        when(request(withRepoProject)).thenReturn(
          Future.successful(
            Response[List[FileCommit]](HttpStatusCodes.BadRequest, FailureResponseBody("Response status: 400"), Map())
          )
        )
        gitLabProjectVersioning.getFileCommits(withRepoProject, path).map {
          _ shouldBe Left(
            VersioningException.FileException("Could not take the file commits. ResponseBody: Response status: 400")
          )
        }
      }
    }

    "getFileVersions" should {
      val dummyCommitJson = List(dummyFileCommit, dummyExistingFileCommit)
      val dummyVersionsJson = dummyGitLabVersion
      val path: Path = Paths.get("tmp/foo.txt")
      val urlEncoder = URLEncoder.encode(path.toString, "UTF-8")
      def projectVersionRequest(project: Project) =
        mockHttpClient.get[Seq[GitLabVersion]](
          url = MockitoMatchers.eq(
            s"${gitLabConfig.url}projects/${project.repository.get.value}/repository/tags"
          ),
          params = MockitoMatchers.any[Map[String, String]],
          headers = MockitoMatchers.eq(gitLabConfig.token)
        )(ec = MockitoMatchers.any[ExecutionContext], f = MockitoMatchers.any[Reads[Seq[GitLabVersion]]])

      def fileCommitsRequest(project: Project) =
        mockHttpClient.get[List[FileCommit]](
          url = MockitoMatchers.eq(
            s"${gitLabConfig.url}projects/${project.repository.get.value}/repository/files/${urlEncoder}"
          ),
          params = MockitoMatchers.any[Map[String, String]],
          headers = MockitoMatchers.eq(gitLabConfig.token)
        )(ec = MockitoMatchers.any[ExecutionContext], f = MockitoMatchers.any[Reads[List[FileCommit]]])

      "return list of Project versions with 200 response" taggedAs Service in {
        when(projectVersionRequest(withRepoProject)).thenReturn(
          Future.successful(
            Response[Seq[GitLabVersion]](HttpStatusCodes.OK, SuccessResponseBody(Seq(dummyVersionsJson)), Map())
          )
        )
        when(fileCommitsRequest(withRepoProject)).thenReturn(
          Future.successful(Response[List[FileCommit]](HttpStatusCodes.OK, SuccessResponseBody(dummyCommitJson), Map()))
        )
        gitLabProjectVersioning.getFileVersions(withRepoProject, path).map {
          _ shouldBe Right(Seq(dummyGitLabVersion))
        }
      }

      "throw new VersioningException" taggedAs Service in {
        val errorText = ""
        when(projectVersionRequest(withRepoProject)).thenReturn(
          Future.successful(
            Response[Seq[GitLabVersion]](HttpStatusCodes.BadRequest, FailureResponseBody(errorText), Map())
          )
        )
        when(fileCommitsRequest(withRepoProject)).thenReturn(
          Future.successful(
            Response[List[FileCommit]](HttpStatusCodes.BadRequest, FailureResponseBody("Response status: 400"), Map())
          )
        )
        gitLabProjectVersioning.getFileCommits(withRepoProject, path).map {
          _ shouldBe Left(
            VersioningException.FileException("Could not take the file commits. ResponseBody: Response status: 400")
          )
        }
      }
    }

    "getFilesTree" should {
      val dummyFilesTree: List[FileTree] = List(dummyFileTree)
      val emptyListValidateResponse = "List((,List(JsonValidationError(List(error.expected.jsarray),WrappedArray())))"
      val version = dummyPipelineVersion
      def request(project: Project, version: Option[PipelineVersion]) = {
        val versionId: Map[String, String] = version match {
          case Some(version) => Map("ref" -> version.name, "recursive" -> "true")
          case None          => Map()
        }
        mockHttpClient.get[List[FileTree]](
          url = MockitoMatchers.eq(s"${gitLabConfig.url}projects/${project.repository.get.value}/repository/tree"),
          params = MockitoMatchers.eq(versionId),
          headers = MockitoMatchers.eq(gitLabConfig.token)
        )(ec = MockitoMatchers.any[ExecutionContext], f = MockitoMatchers.any[Reads[List[FileTree]]])
      }

      "return files tree without version with 200 response" taggedAs Service in {
        when(request(withRepoProject, Option(null))).thenReturn(
          Future.successful(Response[List[FileTree]](HttpStatusCodes.OK, SuccessResponseBody(dummyFilesTree), Map()))
        )
        gitLabProjectVersioning.getFilesTree(withRepoProject, Option(null)).map(_ shouldBe Right(Seq(dummyFileTree)))
      }

      "return files tree with version with 200 response" taggedAs Service in {
        when(request(withRepoProject, Some(version))).thenReturn(
          Future.successful(Response[List[FileTree]](HttpStatusCodes.OK, SuccessResponseBody(dummyFilesTree), Map()))
        )
        gitLabProjectVersioning.getFilesTree(withRepoProject, Some(version)).map(_ shouldBe Right(Seq(dummyFileTree)))
      }

      "return files tree when there is not version" taggedAs Service in {
        when(request(withRepoProject, Some(version))).thenReturn(
          Future.successful(
            Response[List[FileTree]](HttpStatusCodes.OK, FailureResponseBody(emptyListValidateResponse), Map())
          )
        )
        gitLabProjectVersioning
          .getFilesTree(withRepoProject, Some(version))
          .map(
            _ shouldBe Left(
              VersioningException.FileException(
                s"Could not take the files tree or parse json. Response status: 200. ResponseBody: ${emptyListValidateResponse}"
              )
            )
          )
      }

      "throw new VersioningException with 400 response" taggedAs Service in {
        when(request(withRepoProject, Some(version))).thenReturn(
          Future.successful(
            Response[List[FileTree]](
              HttpStatusCodes.BadRequest,
              FailureResponseBody(""),
              Map()
            )
          )
        )
        gitLabProjectVersioning
          .getFilesTree(withRepoProject, Some(version))
          .map(
            _ shouldBe Left(
              VersioningException
                .FileException("Could not take the files tree or parse json. Response status: 400. ResponseBody: ")
            )
          )
      }
    }
  }

  object ProjectContext {
    val EmptyHeaders: Map[String, Seq[String]] = Map()
    lazy val activeProject: Project = TestProjectUtils.getDummyProject()
    lazy val postProject: PostProject = PostProject(name = activeProject.name)
    lazy val repositoryId: RepositoryId = TestProjectUtils.getDummyRepositoryId()
    lazy val inactiveProject: Project = activeProject.copy(active = false)
    lazy val noRepoProject: Project = activeProject.copy(repository = None)
    lazy val withRepoProject: Project =
      activeProject.withRepository(Some(s"${gitLabConfig.idPath}${activeProject.projectId.value}"))
    lazy val withGitlabProject: Project =
      activeProject.withRepository(Some(s"${gitLabConfig.idPath}${repositoryId.id}"))
    lazy val dummyPipelineVersion: PipelineVersion = TestProjectUtils.getDummyPipeLineVersion()
    lazy val dummyPipelineVersionHigher: PipelineVersion = dummyPipelineVersion.increaseMinor
    lazy val dummyGitLabVersion: GitLabVersion = TestProjectUtils.getDummyGitLabVersion()
    lazy val dummyVersion: GitLabVersion = TestProjectUtils.getDummyGitLabVersion()
    lazy val dummyVersions: Seq[GitLabVersion] = Seq(dummyVersion)
    lazy val dummyFileCommit: FileCommit = TestProjectUtils.getDummyFileCommit()
    lazy val dummyExistingFileCommit: FileCommit = FileCommit(dummyGitLabVersion.commit.id)
    lazy val dummyFileTree: FileTree = TestProjectUtils.getDummyFileTree()
  }

  object ProjectFileContext {
    val projectFileContent = ProjectFileContent("Hello world")
    val newFile: ProjectFile = ProjectFile(Paths.get("new_file.txt"), projectFileContent)
    val existFile: ProjectFile = ProjectFile(Paths.get("exist_file.txt"), projectFileContent)
  }
}
