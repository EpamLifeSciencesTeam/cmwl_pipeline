package cromwell.pipeline.service

import cats.data.EitherT
import cats.implicits.toTraverseOps
import cromwell.pipeline.datastorage.dto.File.UpdateFileRequest
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.service.VersioningException._
import cromwell.pipeline.utils.{ GitLabConfig, HttpStatusCodes, URLEncoderUtils }

import java.nio.file.{ Path, Paths }
import scala.concurrent.{ ExecutionContext, Future }

class GitLabProjectVersioning(httpClient: HttpClient, config: GitLabConfig)(implicit ec: ExecutionContext)
    extends ProjectVersioning[VersioningException] {

  override def createRepository(localProject: LocalProject): AsyncResult[Project] =
    if (!localProject.active) {
      Future.failed(RepositoryException("Could not create a repository for deleted project."))
    } else {
      val createRepoUrl: String = s"${config.url}projects"
      val postProject = PostProject(name = localProject.projectId.value)
      httpClient
        .post[GitLabRepositoryResponse, PostProject](url = createRepoUrl, headers = config.token, payload = postProject)
        .map {
          case Response(_, SuccessResponseBody(gitLabResponse), _) =>
            Right(localProject.toProject(gitLabResponse.id, defaultProjectVersion))
          case Response(statusCode, FailureResponseBody(error), _) =>
            Left {
              RepositoryException {
                s"The repository was not created. Response status: $statusCode; Response body [$error]"
              }
            }
        }
        .recover { case e: Throwable => Left(HttpException(e.getMessage)) }
    }

  override def updateFile(
    project: Project,
    projectFile: ProjectFile,
    version: Option[PipelineVersion]
  ): AsyncResult[PipelineVersion] = {
    val path = URLEncoderUtils.encode(projectFile.path.toString)
    val repositoryId: RepositoryId = project.repositoryId
    val fileUrl = s"${config.url}projects/${repositoryId.value}/repository/files/$path"

    getUpdatedProjectVersion(project, version).flatMap {
      case l @ Left(_) => Future.successful(l)
      case Right(newVersion) =>
        val payload = UpdateFileRequest(projectFile.content, newVersion.name, config.defaultBranch)
        httpClient
          .put[UpdateFiledResponse, UpdateFileRequest](fileUrl, payload = payload, headers = config.token)
          .flatMap {
            case Response(_, SuccessResponseBody(_), _) => handleCreateTag(repositoryId, newVersion)
            case _ =>
              httpClient
                .post[UpdateFiledResponse, UpdateFileRequest](fileUrl, payload = payload, headers = config.token)
                .flatMap {
                  case Response(_, SuccessResponseBody(_), _)    => handleCreateTag(repositoryId, newVersion)
                  case Response(_, FailureResponseBody(body), _) => Future.successful(Left(HttpException(body)))
                }
          }
    }
  }

  override def updateFiles(
    project: Project,
    projectFiles: ProjectFiles,
    version: Option[PipelineVersion]
  ): AsyncResult[PipelineVersion] = ???

  override def getFile(project: Project, path: Path, version: Option[PipelineVersion]): AsyncResult[ProjectFile] = {
    val filePath: String = URLEncoderUtils.encode(path.toString)
    val actualFileVersion = version.fold(getLastProjectVersion(project))(Future.successful)

    actualFileVersion.flatMap { fileVersion =>
      httpClient
        .get[GitLabFileContent](
          s"${config.url}projects/${project.repositoryId.value}/repository/files/$filePath",
          Map("ref" -> fileVersion.name),
          config.token
        )
        .map {
          case Response(HttpStatusCodes.OK, SuccessResponseBody(gitLabFile), _) =>
            val content = decodeBase64(gitLabFile.content)
            Right(ProjectFile(path, ProjectFileContent(content)))
          case Response(HttpStatusCodes.OK, FailureResponseBody(error), _) =>
            Left(
              HttpException(
                s"Could not take Project File. Response status: ${HttpStatusCodes.OK}. ResponseBody: $error"
              )
            )
          case Response(responseStatus, _, _) => Left(HttpException(s"Exception. Response status: $responseStatus"))
        }
        .recover { case e: Throwable => Left(HttpException(e.getMessage)) }
    }
  }

  override def getFiles(
    project: Project,
    version: Option[PipelineVersion]
  ): AsyncResult[ProjectFiles] = {
    type EitherF[T] = EitherT[Future, VersioningException, T]

    def getProjectFile(
      project: Project,
      path: Path,
      version: Option[PipelineVersion]
    ): EitherF[ProjectFile] = EitherT(getFile(project, path, version))

    val files = for {
      trees <- EitherT(getFilesTree(project, version))
      files <- trees.toList.traverse(tree => getProjectFile(project, Paths.get(tree.path), version))
    } yield files

    files.value
  }

  override def getProjectVersions(project: Project): AsyncResult[List[PipelineVersion]] =
    getGitLabProjectVersions(project).map(_.map(_.map(_.name))) // nice

  override def getFileVersions(project: Project, path: Path): AsyncResult[List[PipelineVersion]] = {
    val projectVersionsF = getGitLabProjectVersions(project)
    val fileCommitsF = getFileCommits(project, path)
    for {
      projectVersions <- projectVersionsF
      fileCommits <- fileCommitsF
    } yield {
      (projectVersions, fileCommits) match {
        case (Right(tagsProject), Right(tagsFiles)) =>
          Right(for {
            tagProject <- tagsProject
            tagFile <- tagsFiles
            if tagFile.id == tagProject.commit.id
          } yield tagProject.name)
        case (_, Left(exception)) => Left(FileException(exception.getMessage))
        case (Left(exception), _) => Left(FileException(exception.getMessage))
      }
    }
  }

  override def getFileCommits(project: Project, path: Path): AsyncResult[List[Commit]] = {
    val urlEncoder = URLEncoderUtils.encode(path.toString)
    val commitsUrl: String =
      s"${config.url}projects/${project.repositoryId.value}/repository/files/$urlEncoder"
    httpClient.get[List[FileCommit]](url = commitsUrl, headers = config.token).map {
      case Response(_, SuccessResponseBody(commitsSeq), _) => Right(commitsSeq.map(fc => Commit(fc.commitId)))
      case Response(_, FailureResponseBody(error), _) =>
        Left(FileException(s"Could not take the file commits. ResponseBody: $error"))
    }
  }

  private def getGitLabProjectVersions(project: Project): AsyncResult[List[GitLabVersion]] = {
    val versionsListUrl: String = s"${config.url}projects/${project.repositoryId.value}/repository/tags"
    httpClient
      .get[List[GitLabVersion]](url = versionsListUrl, headers = config.token)
      .map {
        case Response(_, SuccessResponseBody(versionSeq), _) => Right(versionSeq)
        case Response(_, FailureResponseBody(error), _) =>
          Left(ProjectException(s"Could not take versions. ResponseBody: $error"))
      }
      .recover { case e: Throwable => Left(HttpException("recover " + e.getMessage)) }
  }

  private def handleCreateTag(repositoryId: RepositoryId, version: PipelineVersion): AsyncResult[PipelineVersion] =
    createTag(repositoryId, version).map(_.map(_ => version))

  private def createTag(repositoryId: RepositoryId, version: PipelineVersion): AsyncResult[SuccessResponseMessage] = {
    val tagUrl = s"${config.url}projects/${repositoryId.value}/repository/tags"
    httpClient
      .post[SuccessResponseMessage, EmptyPayload](
        tagUrl,
        params = Map("tag_name" -> version.name, "ref" -> config.defaultBranch),
        headers = config.token,
        payload = EmptyPayload()
      )
      .map {
        case Response(_, SuccessResponseBody(body), _)  => Right(body)
        case Response(_, FailureResponseBody(error), _) => Left(HttpException(error))
      }
  }

  private def defaultProjectVersion: PipelineVersion = PipelineVersion(config.defaultFileVersion)

  private def getFilesTree(project: Project, version: Option[PipelineVersion]): AsyncResult[Seq[FileTree]] = {
    val versionId: Map[String, String] = version match {
      case Some(version) => Map("ref" -> version.name, "recursive" -> "true")
      case None          => Map()
    }
    val filesTreeUrl: String =
      s"${config.url}projects/${project.repositoryId.value}/repository/tree"

    httpClient
      .get[List[FileTree]](url = filesTreeUrl, params = versionId, headers = config.token)
      .map {
        case Response(_, SuccessResponseBody(fileTreeSeq), _) => Right(fileTreeSeq)
        case Response(statusCode, FailureResponseBody(error), _) =>
          Left(
            FileException(
              s"Could not take the files tree or parse json. Response status: $statusCode. ResponseBody: $error"
            )
          )
      }
      .recover { case e: Throwable => Left(HttpException(e.getMessage)) }

  }

  private def getUpdatedProjectVersion(
    project: Project,
    optionUserVersion: Option[PipelineVersion]
  ): AsyncResult[PipelineVersion] =
    getLastProjectVersion(project).map(projectVersion => getNewProjectVersion(projectVersion, optionUserVersion))

  private def getLastProjectVersion(project: Project): Future[PipelineVersion] =
    getProjectVersions(project).flatMap {
      case Left(error)        => Future.failed(error)
      case Right(List(v, _*)) => Future.successful(v)
      case Right(List())      => Future.successful(project.version)
    }

  private def getNewProjectVersion(
    projectVersion: PipelineVersion,
    optionUserVersion: Option[PipelineVersion]
  ): Either[VersioningException, PipelineVersion] =
    optionUserVersion match {
      case None => Right(projectVersion.increaseRevision)
      case Some(userVersion) if projectVersion >= userVersion =>
        Left(ProjectException(s"Your version $userVersion is out of date. Current version of project: $projectVersion"))
      case Some(userVersion) => Right(userVersion)
    }

  private def decodeBase64(str: String): String = new String(java.util.Base64.getDecoder.decode(str))
}
