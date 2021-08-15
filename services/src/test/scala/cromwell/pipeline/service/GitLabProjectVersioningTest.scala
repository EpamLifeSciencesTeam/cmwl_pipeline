package cromwell.pipeline.service

import akka.http.scaladsl.model.StatusCodes
import cromwell.pipeline.datastorage.dao.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto.File.UpdateFileRequest
import cromwell.pipeline.datastorage.dto.PipelineVersion.PipelineVersionException
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.utils._
import java.nio.file.{ Path, Paths }
import org.mockito.Matchers.{ any, eq => exact }
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Reads
import scala.concurrent.{ ExecutionContext, Future }

class GitLabProjectVersioningTest extends AsyncWordSpec with Matchers with MockitoSugar {

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

      def postNewProject(postProject: PostProject): Future[Response[GitLabRepositoryResponse]] =
        mockHttpClient.post[GitLabRepositoryResponse, PostProject](
          url = gitLabConfig.url + "projects",
          headers = gitLabConfig.token,
          payload = postProject
        )

      "throw new VersioningException for inactive project" taggedAs Service in {
        gitLabProjectVersioning.createRepository(inactiveLocalProject).failed.map {
          _ shouldBe VersioningException.RepositoryException("Could not create a repository for deleted project.")
        }
      }

      "return new active Project with 201 response" taggedAs Service in {

        when(postNewProject(postProject)).thenReturn {
          Future.successful {
            Response[GitLabRepositoryResponse](
              HttpStatusCodes.Created,
              SuccessResponseBody[GitLabRepositoryResponse](gitLabRepositoryResponse),
              EmptyHeaders
            )
          }
        }

        gitLabProjectVersioning.createRepository(activeLocalProject).map {
          _ shouldBe Right(activeProject)
        }
      }

      "throw new VersioningException with 400 response" taggedAs Service in {
        val errorMsg = "The repository was not created. Response status: 400"
        when(postNewProject(postProject)).thenReturn {
          Future.successful {
            Response[GitLabRepositoryResponse](
              HttpStatusCodes.BadRequest,
              FailureResponseBody(errorMsg),
              EmptyHeaders
            )
          }
        }
        gitLabProjectVersioning.createRepository(activeLocalProject).map {
          _ shouldBe Left {
            VersioningException.RepositoryException {
              s"The repository was not created. Response status: 400; Response body [$errorMsg]"
            }
          }
        }
      }
    }

    "getProjectVersions" should {
      def getProjectVersions(project: Project): Future[Response[Seq[GitLabVersion]]] =
        mockHttpClient.get[Seq[GitLabVersion]](
          url = exact(gitLabConfig.url + "projects/" + project.repositoryId.value + "/repository/tags"),
          headers = any[Map[String, String]],
          params = any[Map[String, String]]
        )(any[ExecutionContext], any[Reads[Seq[GitLabVersion]]])

      "return list of Project versions with 200 response" taggedAs Service in {
        when(getProjectVersions(projectWithRepo)).thenReturn {
          Future.successful {
            Response[Seq[GitLabVersion]](
              HttpStatusCodes.OK,
              SuccessResponseBody[Seq[GitLabVersion]](dummyVersions),
              EmptyHeaders
            )
          }
        }
        gitLabProjectVersioning.getProjectVersions(projectWithRepo).map {
          _ shouldBe Right(Seq(dummyPipelineVersion))
        }
      }

      "throw new VersioningException with 400 response" taggedAs Service in {
        val activeProject: Project = TestProjectUtils.getDummyProject()
        when(getProjectVersions(activeProject)).thenReturn {
          Future.successful {
            Response[Seq[GitLabVersion]](
              HttpStatusCodes.BadRequest,
              FailureResponseBody("Response status: 400"),
              EmptyHeaders
            )
          }
        }
        gitLabProjectVersioning.getProjectVersions(activeProject).map {
          _ shouldBe Left {
            VersioningException.ProjectException("Could not take versions. ResponseBody: Response status: 400")
          }
        }
      }
    }

    "updateFile" should {
      val successCreateMessage = "File was created"
      val tagUrl = s"${gitLabConfig.url}projects/${projectWithRepo.repositoryId.value}/repository/tags"
      val gitlabVersion = TestProjectUtils.getDummyGitLabVersion(dummyPipelineVersion)
      when {
        mockHttpClient.post[SuccessResponseMessage, PipelineVersion](
          url = tagUrl,
          params = Map("tag_name" -> dummyPipelineVersionHigher.name, "ref" -> gitLabConfig.defaultBranch),
          headers = gitLabConfig.token,
          payload = dummyPipelineVersion
        )
      }.thenReturn {
        Future.successful {
          Response[SuccessResponseMessage](
            StatusCodes.OK.intValue,
            SuccessResponseBody(SuccessResponseMessage("Tag was created")),
            EmptyHeaders
          )
        }
      }

      "succeed when file is new" taggedAs Service in {
        val path = URLEncoderUtils.encode(newFile.path.toString)
        val url = s"${gitLabConfig.url}projects/${projectWithRepo.repositoryId.value}/repository/files/$path"
        val payload =
          File.UpdateFileRequest(newFile.content, dummyPipelineVersionHigher.name, gitLabConfig.defaultBranch)

        when {
          mockHttpClient.get[List[GitLabVersion]](
            url = tagUrl,
            headers = gitLabConfig.token
          )
        }.thenReturn {
          Future.successful {
            Response[List[GitLabVersion]](
              StatusCodes.OK.intValue,
              SuccessResponseBody(List(gitlabVersion)),
              EmptyHeaders
            )
          }
        }
        when {
          mockHttpClient.put[UpdateFiledResponse, UpdateFileRequest](
            url = url,
            headers = gitLabConfig.token,
            payload = payload
          )
        }.thenReturn {
          Future.successful {
            Response[UpdateFiledResponse](
              StatusCodes.BadRequest.intValue,
              FailureResponseBody("File does not exist"),
              EmptyHeaders
            )
          }
        }
        when {
          mockHttpClient.post[UpdateFiledResponse, UpdateFileRequest](
            url = url,
            headers = gitLabConfig.token,
            payload = payload
          )
        }.thenReturn {
          Future.successful(
            Response[UpdateFiledResponse](
              StatusCodes.OK.intValue,
              SuccessResponseBody(UpdateFiledResponse(newFile.path.toString, "master")),
              EmptyHeaders
            )
          )
        }
        when {
          mockHttpClient.post[SuccessResponseMessage, EmptyPayload](
            url = tagUrl,
            params = Map("tag_name" -> dummyPipelineVersionHigher.name, "ref" -> gitLabConfig.defaultBranch),
            headers = gitLabConfig.token,
            payload = EmptyPayload()
          )
        }.thenReturn {
          Future.successful {
            Response[SuccessResponseMessage](
              StatusCodes.OK.intValue,
              SuccessResponseBody(SuccessResponseMessage(successCreateMessage)),
              EmptyHeaders
            )
          }
        }

        gitLabProjectVersioning.updateFile(projectWithRepo, newFile, Some(dummyPipelineVersionHigher)).map {
          _ shouldBe Right(dummyPipelineVersionHigher)
        }
      }

      "return update file response when file is already exist" taggedAs Service in {
        val payload =
          File.UpdateFileRequest(existFile.content, dummyPipelineVersionHigher.name, gitLabConfig.defaultBranch)
        val path = URLEncoderUtils.encode(existFile.path.toString)
        val url =
          s"${gitLabConfig.url}projects/${projectWithRepo.repositoryId.value}/repository/files/$path"
        when(mockHttpClient.get[List[GitLabVersion]](tagUrl, headers = gitLabConfig.token)).thenReturn {
          Future.successful {
            Response[List[GitLabVersion]](
              StatusCodes.OK.intValue,
              SuccessResponseBody(List(gitlabVersion)),
              EmptyHeaders
            )
          }
        }
        when {
          mockHttpClient.put[UpdateFiledResponse, UpdateFileRequest](
            url = url,
            headers = gitLabConfig.token,
            payload = payload
          )
        }.thenReturn {
          Future.successful {
            Response[UpdateFiledResponse](
              StatusCodes.OK.intValue,
              SuccessResponseBody(UpdateFiledResponse(newFile.path.toString, "master")),
              EmptyHeaders
            )
          }
        }
        gitLabProjectVersioning.updateFile(projectWithRepo, existFile, Some(dummyPipelineVersionHigher)).map {
          _ shouldBe Right(dummyPipelineVersionHigher)
        }
      }

      "return exception if version is invalid" taggedAs Service in {
        val thrown = the[PipelineVersionException] thrownBy
          gitLabProjectVersioning.updateFile(projectWithRepo, existFile, Some(PipelineVersion("1")))
        thrown.message should equal("Format of version name: 'v(int).(int).(int)', but got: 1")
      }
    }

    "getFile" should {
      "return file with 200 response" taggedAs Service in {
        val path = Paths.get("test.md")
        val encodedPathStr = URLEncoderUtils.encode(path.toString)
        when {
          mockHttpClient.get[GitLabFileContent](
            s"${gitLabConfig.url}/projects/${activeProject.repositoryId.value}/repository/files/$encodedPathStr",
            Map("ref" -> dummyPipelineVersion.name),
            gitLabConfig.token
          )
        }.thenReturn {
          Future.successful {
            Response[GitLabFileContent](
              HttpStatusCodes.OK,
              SuccessResponseBody(GitLabFileContent("VGVzdCBGaWxl")),
              EmptyHeaders
            )
          }
        }

        gitLabProjectVersioning
          .getFile(activeProject, path, Some(dummyPipelineVersion))
          .map(_ shouldBe Right(ProjectFile(path, ProjectFileContent("Test File"))))
      }

      "throw new VersioningException with not 200 response" taggedAs Service in {
        val path = Paths.get("test.md")
        val encodedPathStr = URLEncoderUtils.encode(path.toString)
        when {
          mockHttpClient.get[GitLabFileContent](
            s"${gitLabConfig.url}/projects/${activeProject.repositoryId.value}/repository/files/$encodedPathStr",
            Map("ref" -> dummyPipelineVersion.name),
            gitLabConfig.token
          )
        }.thenReturn {
          Future.successful {
            Response[GitLabFileContent](HttpStatusCodes.NotFound, FailureResponseBody("Not Found"), EmptyHeaders)
          }
        }

        gitLabProjectVersioning
          .getFile(activeProject, path, Some(dummyPipelineVersion))
          .map(_ shouldBe Left(VersioningException.HttpException("Exception. Response status: 404")))
      }
    }

    "getFiles" should {
      val dummyFilesTree: List[FileTree] = List(dummyFileTree)
      val path = Paths.get(dummyFileTree.path)
      val encodedPathStr = URLEncoderUtils.encode(path.toString)
      val version = dummyPipelineVersion

      def getFileTrees(project: Project, version: Option[PipelineVersion]): Future[Response[List[FileTree]]] = {
        val versionId: Map[String, String] = version match {
          case Some(version) => Map("ref" -> version.name, "recursive" -> "true")
          case None          => Map()
        }
        mockHttpClient.get[List[FileTree]](
          url = exact(s"${gitLabConfig.url}projects/${project.repositoryId.value}/repository/tree"),
          params = exact(versionId),
          headers = exact(gitLabConfig.token)
        )(ec = any[ExecutionContext], f = any[Reads[List[FileTree]]])
      }

      "return files with 200 response" taggedAs Service in {
        when(getFileTrees(activeProject, Some(version))).thenReturn {
          Future.successful(Response[List[FileTree]](HttpStatusCodes.OK, SuccessResponseBody(dummyFilesTree), Map()))
        }

        when {
          mockHttpClient.get[GitLabFileContent](
            s"${gitLabConfig.url}/projects/${activeProject.repositoryId.value}/repository/files/$encodedPathStr",
            Map("ref" -> version.name),
            gitLabConfig.token
          )
        }.thenReturn {
          Future.successful {
            Response[GitLabFileContent](
              HttpStatusCodes.OK,
              SuccessResponseBody(GitLabFileContent("VGVzdCBGaWxl")),
              EmptyHeaders
            )
          }
        }

        gitLabProjectVersioning
          .getFiles(activeProject, Some(version))
          .map(_ shouldBe Right(List(ProjectFile(path, ProjectFileContent("Test File")))))
      }

      "throw new VersioningException with not 200 response" taggedAs Service in {
        when(getFileTrees(activeProject, Some(version))).thenReturn {
          Future.successful(Response[List[FileTree]](HttpStatusCodes.OK, SuccessResponseBody(dummyFilesTree), Map()))
        }
        when {
          mockHttpClient.get[GitLabFileContent](
            s"${gitLabConfig.url}/projects/${activeProject.repositoryId.value}/repository/files/$encodedPathStr",
            Map("ref" -> dummyPipelineVersion.name),
            gitLabConfig.token
          )
        }.thenReturn {
          Future.successful {
            Response[GitLabFileContent](HttpStatusCodes.NotFound, FailureResponseBody("Not Found"), EmptyHeaders)
          }
        }

        gitLabProjectVersioning
          .getFiles(activeProject, Some(version))
          .map(_ shouldBe Left(VersioningException.HttpException("Exception. Response status: 404")))
      }
    }

    "getFileCommits" should {
      val dummyCommitList = List(dummyFileCommit)
      val path: Path = Paths.get("tmp/foo.txt")
      val urlEncoder = URLEncoderUtils.encode(path.toString)
      def getFileCommits(project: Project): Future[Response[List[FileCommit]]] =
        mockHttpClient.get[List[FileCommit]](
          url = exact(s"${gitLabConfig.url}projects/${project.repositoryId.value}/repository/files/$urlEncoder"),
          params = any[Map[String, String]],
          headers = exact(gitLabConfig.token)
        )(ec = any[ExecutionContext], f = any[Reads[List[FileCommit]]])

      "return list of file commits with 200 response" taggedAs Service in {
        when(getFileCommits(projectWithRepo)).thenReturn {
          Future.successful(Response(HttpStatusCodes.OK, SuccessResponseBody(dummyCommitList), Map()))
        }
        gitLabProjectVersioning.getFileCommits(projectWithRepo, path).map {
          _ shouldBe Right(Seq(dummyCommit))
        }
      }

      "throw new VersioningException with 400 response" taggedAs Service in {
        when(getFileCommits(projectWithRepo)).thenReturn {
          Future.successful {
            Response[List[FileCommit]](HttpStatusCodes.BadRequest, FailureResponseBody("Response status: 400"), Map())
          }
        }
        gitLabProjectVersioning.getFileCommits(projectWithRepo, path).map {
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
      val urlEncoder = URLEncoderUtils.encode(path.toString)
      def projectVersionRequest(project: Project): Future[Response[Seq[GitLabVersion]]] =
        mockHttpClient.get[Seq[GitLabVersion]](
          url = exact(s"${gitLabConfig.url}projects/${project.repositoryId.value}/repository/tags"),
          params = any[Map[String, String]],
          headers = exact(gitLabConfig.token)
        )(ec = any[ExecutionContext], f = any[Reads[Seq[GitLabVersion]]])

      def fileCommitsRequest(project: Project): Future[Response[List[FileCommit]]] =
        mockHttpClient.get[List[FileCommit]](
          url = exact(s"${gitLabConfig.url}projects/${project.repositoryId.value}/repository/files/$urlEncoder"),
          params = any[Map[String, String]],
          headers = exact(gitLabConfig.token)
        )(ec = any[ExecutionContext], f = any[Reads[List[FileCommit]]])

      "return list of Project versions with 200 response" taggedAs Service in {
        when(projectVersionRequest(projectWithRepo)).thenReturn {
          Future.successful {
            Response[Seq[GitLabVersion]](HttpStatusCodes.OK, SuccessResponseBody(Seq(dummyVersionsJson)), Map())
          }
        }
        when(fileCommitsRequest(projectWithRepo)).thenReturn {
          Future.successful(Response[List[FileCommit]](HttpStatusCodes.OK, SuccessResponseBody(dummyCommitJson), Map()))
        }
        gitLabProjectVersioning.getFileVersions(projectWithRepo, path).map {
          _ shouldBe Right(Seq(dummyGitLabVersion.name))
        }
      }

      "throw new VersioningException" taggedAs Service in {
        val errorText = ""
        when(projectVersionRequest(projectWithRepo)).thenReturn {
          Future.successful {
            Response[Seq[GitLabVersion]](HttpStatusCodes.BadRequest, FailureResponseBody(errorText), Map())
          }
        }
        when(fileCommitsRequest(projectWithRepo)).thenReturn {
          Future.successful {
            Response[List[FileCommit]](HttpStatusCodes.BadRequest, FailureResponseBody("Response status: 400"), Map())
          }
        }
        gitLabProjectVersioning.getFileCommits(projectWithRepo, path).map {
          _ shouldBe Left {
            VersioningException.FileException("Could not take the file commits. ResponseBody: Response status: 400")
          }
        }
      }
    }

  }

  object ProjectContext {
    val EmptyHeaders: Map[String, Seq[String]] = Map()
    lazy val gitLabRepositoryResponse: GitLabRepositoryResponse = TestProjectUtils.getDummyGitLabRepositoryResponse()
    lazy val defaultVersion: PipelineVersion = PipelineVersion(gitLabConfig.defaultFileVersion)

    lazy val activeLocalProject: LocalProject = TestProjectUtils.getDummyLocalProject().copy(active = true)
    lazy val inactiveLocalProject: LocalProject = TestProjectUtils.getDummyLocalProject(active = false)
    lazy val activeProject: Project = activeLocalProject.toProject(gitLabRepositoryResponse.id, defaultVersion)
    lazy val inactiveProject: Project = activeProject.copy(active = false)
    lazy val projectWithRepo: Project = activeLocalProject.toProject(gitLabRepositoryResponse.id, defaultVersion)

    lazy val postProject: PostProject = PostProject(name = activeProject.projectId.value)

    lazy val dummyPipelineVersion: PipelineVersion = TestProjectUtils.getDummyPipeLineVersion()
    lazy val dummyPipelineVersionHigher: PipelineVersion = dummyPipelineVersion.increaseMinor
    lazy val dummyGitLabVersion: GitLabVersion = TestProjectUtils.getDummyGitLabVersion()
    lazy val dummyVersion: GitLabVersion = TestProjectUtils.getDummyGitLabVersion(dummyPipelineVersion)
    lazy val dummyVersions: Seq[GitLabVersion] = Seq(dummyVersion)
    lazy val dummyFileCommit: FileCommit = TestProjectUtils.getDummyFileCommit()
    lazy val dummyCommit: Commit = Commit(dummyFileCommit.commitId)
    lazy val dummyExistingFileCommit: FileCommit = FileCommit(dummyGitLabVersion.commit.id)
    lazy val dummyFileTree: FileTree = TestProjectUtils.getDummyFileTree()
  }

  object ProjectFileContext {
    private val projectFileContent = ProjectFileContent("Hello world")
    val newFile: ProjectFile = ProjectFile(Paths.get("new_file.txt"), projectFileContent)
    val existFile: ProjectFile = ProjectFile(Paths.get("exist_file.txt"), projectFileContent)
  }
}
