package cromwell.pipeline.service

import akka.http.scaladsl.model.StatusCodes
import cromwell.pipeline.datastorage.dao.utils.TestProjectUtils
import cromwell.pipeline.datastorage.dto.File.UpdateFileRequest
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.utils._
import org.mockito.Matchers.{ any, eq => exact }
import org.mockito.Mockito.{ reset, when }
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterEach, Matchers }
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Reads

import java.nio.file.{ Path, Paths }
import scala.concurrent.{ ExecutionContext, Future }

class GitLabProjectVersioningTest extends AsyncWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  val mockHttpClient: HttpClient = mock[HttpClient]
  val gitLabConfig: GitLabConfig = ApplicationConfig.load().gitLabConfig
  val gitLabProjectVersioning: GitLabProjectVersioning = new GitLabProjectVersioning(mockHttpClient, gitLabConfig)

  override protected def afterEach(): Unit = {
    reset(mockHttpClient)
    super.afterEach()
  }

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

    "updateFile" should {
      val successCreateMessage = "File was created"
      val tagUrl = s"${gitLabConfig.url}projects/${projectWithRepo.repositoryId.value}/repository/tags"
      val gitlabVersion = TestProjectUtils.getDummyGitLabVersion(dummyPipelineVersion)
      val gitlabVersionHigher = TestProjectUtils.getDummyGitLabVersion(dummyPipelineVersionHigher)

      def configureGetVersions(versions: List[GitLabVersion]): Unit =
        when {
          mockHttpClient.get[List[GitLabVersion]](
            url = exact(tagUrl),
            params = any[Map[String, String]],
            headers = exact(gitLabConfig.token)
          )(any[ExecutionContext], any[Reads[List[GitLabVersion]]])
        }.thenReturn(Future.successful(Response(StatusCodes.OK.intValue, SuccessResponseBody(versions), EmptyHeaders)))

      def configurePutFile(url: String, payload: UpdateFileRequest, response: Response[UpdateFiledResponse]): Unit =
        when {
          mockHttpClient.put[UpdateFiledResponse, UpdateFileRequest](
            url = url,
            headers = gitLabConfig.token,
            payload = payload
          )
        }.thenReturn(Future.successful(response))

      def configurePostFile(url: String, payload: UpdateFileRequest, response: Response[UpdateFiledResponse]): Unit =
        when {
          mockHttpClient.put[UpdateFiledResponse, UpdateFileRequest](
            url = url,
            headers = gitLabConfig.token,
            payload = payload
          )
        }.thenReturn(Future.successful(response))

      def configureCreateTag(version: PipelineVersion, response: SuccessResponseMessage): Unit =
        when {
          mockHttpClient.post[SuccessResponseMessage, EmptyPayload](
            url = tagUrl,
            params = Map("tag_name" -> version.name, "ref" -> gitLabConfig.defaultBranch),
            headers = gitLabConfig.token,
            payload = EmptyPayload()
          )
        }.thenReturn(Future.successful(Response(StatusCodes.OK.intValue, SuccessResponseBody(response), EmptyHeaders)))

      "succeed when file is new" taggedAs Service in {
        val payload = UpdateFileRequest(newFile.content, dummyPipelineVersionHigher.name, gitLabConfig.defaultBranch)
        val path = URLEncoderUtils.encode(newFile.path.toString)
        val url = s"${gitLabConfig.url}projects/${projectWithRepo.repositoryId.value}/repository/files/$path"

        val putResponse =
          Response[UpdateFiledResponse](
            StatusCodes.BadRequest.intValue,
            FailureResponseBody("File does not exist"),
            EmptyHeaders
          )

        val postResponse =
          Response[UpdateFiledResponse](
            StatusCodes.OK.intValue,
            SuccessResponseBody(UpdateFiledResponse(newFile.path.toString, "master")),
            EmptyHeaders
          )

        configureGetVersions(List(gitlabVersion))
        configurePutFile(url, payload, putResponse)
        configurePostFile(url, payload, postResponse)
        configureCreateTag(dummyPipelineVersionHigher, SuccessResponseMessage(successCreateMessage))

        gitLabProjectVersioning.updateFile(projectWithRepo, newFile, Some(dummyPipelineVersionHigher)).map {
          _ shouldBe Right(dummyPipelineVersionHigher)
        }
      }

      "succeed when file already exists" taggedAs Service in {
        val payload = UpdateFileRequest(existFile.content, dummyPipelineVersionHigher.name, gitLabConfig.defaultBranch)
        val path = URLEncoderUtils.encode(existFile.path.toString)
        val url = s"${gitLabConfig.url}projects/${projectWithRepo.repositoryId.value}/repository/files/$path"

        val putResponse = Response[UpdateFiledResponse](
          StatusCodes.OK.intValue,
          SuccessResponseBody(UpdateFiledResponse(newFile.path.toString, "master")),
          EmptyHeaders
        )

        configureGetVersions(List(gitlabVersion))
        configurePutFile(url, payload, putResponse)
        configureCreateTag(dummyPipelineVersionHigher, SuccessResponseMessage(successCreateMessage))

        gitLabProjectVersioning.updateFile(projectWithRepo, existFile, Some(dummyPipelineVersionHigher)).map {
          _ shouldBe Right(dummyPipelineVersionHigher)
        }
      }

      "succeed if user didn't provide version" taggedAs Service in {
        val updatedVersion = dummyPipelineVersion.increaseRevision
        val payload = UpdateFileRequest(existFile.content, updatedVersion.name, gitLabConfig.defaultBranch)
        val path = URLEncoderUtils.encode(existFile.path.toString)
        val url = s"${gitLabConfig.url}projects/${projectWithRepo.repositoryId.value}/repository/files/$path"

        val putResponse = Response[UpdateFiledResponse](
          StatusCodes.OK.intValue,
          SuccessResponseBody(UpdateFiledResponse(existFile.path.toString, "master")),
          EmptyHeaders
        )

        // have to duplicate because of internal bug in cromwell.pipeline.service.GitLabProjectVersioning.getLastProjectVersion
        configureGetVersions(List(gitlabVersion, gitlabVersion))
        configurePutFile(url, payload, putResponse)
        configureCreateTag(updatedVersion, SuccessResponseMessage(successCreateMessage))

        gitLabProjectVersioning.updateFile(projectWithRepo, existFile, None).map {
          _ shouldBe Right(updatedVersion)
        }
      }

      "fail if user version obsolete" taggedAs Service in {
        // have to duplicate because of internal bug in cromwell.pipeline.service.GitLabProjectVersioning.getLastProjectVersion
        configureGetVersions(List(gitlabVersionHigher, gitlabVersionHigher))

        gitLabProjectVersioning.updateFile(projectWithRepo, existFile, Some(dummyPipelineVersion)).map {
          val errMsg =
            s"Your version $dummyPipelineVersion is out of date. Current version of project: $dummyPipelineVersionHigher"
          _ shouldBe Left(VersioningException.ProjectException(errMsg))
        }
      }

      "fail if failed to fetch project versions" taggedAs Service in {
        val errMsg = "something something bad request"
        when {
          mockHttpClient.get[List[GitLabVersion]](
            url = exact(tagUrl),
            params = any[Map[String, String]],
            headers = exact(gitLabConfig.token)
          )(any[ExecutionContext], any[Reads[List[GitLabVersion]]])
        }.thenReturn(Future.failed(new RuntimeException(errMsg)))

        gitLabProjectVersioning.updateFile(projectWithRepo, existFile, Some(dummyPipelineVersion)).failed.map {
          _ should have.message(s"recover $errMsg")
        }
      }
    }

    "getFile" should {
      "return file with 200 response" taggedAs Service in {
        val path = Paths.get("test.md")
        val encodedPathStr = URLEncoderUtils.encode(path.toString)
        when {
          mockHttpClient.get[GitLabFileContent](
            s"${gitLabConfig.url}projects/${activeProject.repositoryId.value}/repository/files/$encodedPathStr",
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
            s"${gitLabConfig.url}projects/${activeProject.repositoryId.value}/repository/files/$encodedPathStr",
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

      def getFile(
        project: Project,
        filePath: String,
        version: Option[PipelineVersion]
      ): Future[Response[GitLabFileContent]] =
        mockHttpClient.get[GitLabFileContent](
          exact(s"${gitLabConfig.url}projects/${project.repositoryId.value}/repository/files/$filePath"),
          exact(Map("ref" -> version.map(_.name).getOrElse(project.version.name))),
          exact(gitLabConfig.token)
        )(ec = any[ExecutionContext], f = any[Reads[GitLabFileContent]])

      def getGitLabProjectVersions(project: Project): Future[Response[List[GitLabVersion]]] =
        mockHttpClient.get[List[GitLabVersion]](
          exact(s"${gitLabConfig.url}projects/${project.repositoryId.value}/repository/tags"),
          any[Map[String, String]],
          exact(gitLabConfig.token)
        )(ec = any[ExecutionContext], f = any[Reads[List[GitLabVersion]]])

      "return files with version with 200 response" taggedAs Service in {
        when(getFileTrees(activeProject, Some(version))).thenReturn {
          Future.successful(Response(HttpStatusCodes.OK, SuccessResponseBody(dummyFilesTree), Map()))
        }

        when(getFile(activeProject, encodedPathStr, Some(version))).thenReturn {
          Future.successful {
            Response(HttpStatusCodes.OK, SuccessResponseBody(GitLabFileContent("VGVzdCBGaWxl")), EmptyHeaders)
          }
        }

        gitLabProjectVersioning
          .getFiles(activeProject, Some(version))
          .map(_ shouldBe Right(List(ProjectFile(path, ProjectFileContent("Test File")))))
      }

      "return files without version with 200 response" taggedAs Service in {
        when(getGitLabProjectVersions(activeProject)).thenReturn {
          Future.successful {
            Response(
              HttpStatusCodes.OK,
              SuccessResponseBody(List(GitLabVersion(Commit("some commit"), activeProject.version))),
              EmptyHeaders
            )
          }
        }

        when(getFileTrees(activeProject, None)).thenReturn {
          Future.successful(Response(HttpStatusCodes.OK, SuccessResponseBody(dummyFilesTree), Map()))
        }

        when(getFile(activeProject, encodedPathStr, None)).thenReturn {
          Future.successful {
            Response(HttpStatusCodes.OK, SuccessResponseBody(GitLabFileContent("VGVzdCBGaWxl")), EmptyHeaders)
          }
        }

        gitLabProjectVersioning
          .getFiles(activeProject, None)
          .map(_ shouldBe Right(List(ProjectFile(path, ProjectFileContent("Test File")))))
      }

      "throw new VersioningException with not 200 response" taggedAs Service in {
        when(getFileTrees(activeProject, Some(version))).thenReturn {
          Future.successful(Response[List[FileTree]](HttpStatusCodes.OK, SuccessResponseBody(dummyFilesTree), Map()))
        }
        when {
          mockHttpClient.get[GitLabFileContent](
            s"${gitLabConfig.url}projects/${activeProject.repositoryId.value}/repository/files/$encodedPathStr",
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

//    "getUpdatedProjectVersion" should {
//      val userVersion = PipelineVersion("v0.0.1")
//      val project = TestProjectUtils.getDummyProject()
//      def getUpdatedProject(project: Project): Future[Response[Seq[GitLabVersion]]] =
//        mockHttpClient.get[Seq[GitLabVersion]](
//          url = exact(gitLabConfig.url + "projects/" + project.repositoryId.value + "/repository/tags"),
//          headers = any[Map[String, String]],
//          params = any[Map[String, String]]
//        )(any[ExecutionContext], any[Reads[Seq[GitLabVersion]]])
//
//      "return new version received from user" taggedAs Service in {
//
//        when(getUpdatedProject(project)).thenReturn {
//          Future.successful {
//            Response[Seq[GitLabVersion]](
//              HttpStatusCodes.OK,
//              SuccessResponseBody[Seq[GitLabVersion]](dummyVersions),
//              EmptyHeaders
//            )
//          }
//        }
//
//        gitLabProjectVersioning.getUpdatedProjectVersion(project, Some(userVersion)).map(_ shouldBe Right(userVersion))
//      }
//
//      "fail with VersioningException.ProjectException" taggedAs Service in {
//        val activeProject: Project = TestProjectUtils.getDummyProject()
//        when(getUpdatedProject(activeProject)).thenReturn {
//          Future.successful {
//            Response[Seq[GitLabVersion]](
//              HttpStatusCodes.BadRequest,
//              FailureResponseBody("Response status: 400"),
//              EmptyHeaders
//            )
//          }
//        }
//        gitLabProjectVersioning
//          .getUpdatedProjectVersion(project, None)
//          .map(
//            _ shouldBe Left(
//              VersioningException.ProjectException(
//                "Can't take decision what version should it be: no version from user and no version in project yet"
//              )
//            )
//          )
//      }
//    }

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
