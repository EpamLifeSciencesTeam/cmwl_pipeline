package cromwell.pipeline.service

import cats.data.EitherT
import cats.implicits.toTraverseOps
import cromwell.pipeline.datastorage.dto.File.UpdateFileRequest
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.utils.{ GitLabConfig, HttpStatusCodes, URLEncoderUtils }
import java.nio.file.{ Path, Paths }
import scala.concurrent.{ ExecutionContext, Future }

class GitLabProjectVersioning(httpClient: HttpClient, config: GitLabConfig)
    extends ProjectVersioning[VersioningException] {

  def getUpdatedProjectVersion(project: Project, optionUserVersion: Option[PipelineVersion])(
    implicit ec: ExecutionContext
  ): AsyncResult[PipelineVersion] =
    getLastProjectVersion(project).map(projectVersion => getNewProjectVersion(projectVersion, optionUserVersion))

  private def getLastProjectVersion(project: Project)(implicit ec: ExecutionContext): Future[Option[PipelineVersion]] =
    getProjectVersions(project).flatMap {
      case Left(error)                      => Future.failed(error)
      case Right(Seq(_))                    => Future.successful(None)
      case Right(Seq(v: GitLabVersion, _*)) => Future.successful(Some(v.name))
      case Right(Seq())                     => Future.successful(Some(getDefaultProjectVersion()))
    }

  private def getNewProjectVersion(
    optionProjectVersion: Option[PipelineVersion],
    optionUserVersion: Option[PipelineVersion]
  ): Either[VersioningException, PipelineVersion] =
    (optionProjectVersion, optionUserVersion) match {
      case (Some(projectVersion), Some(userVersion)) =>
        if (projectVersion >= userVersion) {
          Left {
            VersioningException.ProjectException {
              s"Your version $userVersion is out of date. Current version of project: $projectVersion"
            }
          }
        } else {
          Right(userVersion)
        }
      case (Some(projectVersion), None) => Right(projectVersion.increaseRevision)
      case (None, Some(userVersion))    => Right(userVersion)
      case (None, None) =>
        Left(
          VersioningException.ProjectException(
            "Can't take decision what version should it be: no version from user and no version in project yet"
          )
        )
    }

  private def handleCreateTag(
    repositoryId: RepositoryId,
    version: PipelineVersion,
    responseBody: UpdateFiledResponse
  )(
    implicit ec: ExecutionContext
  ): AsyncResult[UpdateFiledResponse] =
    createTag(repositoryId, version).map {
      case Right(_)        => Right(responseBody)
      case Left(exception) => Left(exception)
    }

  override def updateFile(project: Project, projectFile: ProjectFile, version: PipelineVersion)(
    implicit ec: ExecutionContext
  ): AsyncResult[UpdateFiledResponse] = {
    val path = URLEncoderUtils.encode(projectFile.path.toString)
    val repositoryId: RepositoryId = project.repositoryId
    val fileUrl = s"${config.url}projects/${repositoryId.value}/repository/files/$path"

    val payload = UpdateFileRequest(projectFile.content, version.toString, config.defaultBranch)
    httpClient.put[UpdateFiledResponse, UpdateFileRequest](fileUrl, payload = payload, headers = config.token).flatMap {
      case Response(_, SuccessResponseBody(body), _) =>
        handleCreateTag(repositoryId, version, body)
      case _ =>
        httpClient
          .post[UpdateFiledResponse, UpdateFileRequest](fileUrl, payload = payload, headers = config.token)
          .flatMap {
            case Response(_, SuccessResponseBody(body), _) =>
              handleCreateTag(repositoryId, version, body)
            case Response(_, FailureResponseBody(body), _) =>
              Future.successful(Left(VersioningException.HttpException(body)))
          }
    }
  }

  private def createTag(repositoryId: RepositoryId, version: PipelineVersion)(
    implicit ec: ExecutionContext
  ): AsyncResult[SuccessResponseMessage] = {
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
        case Response(_, FailureResponseBody(error), _) => Left(VersioningException.HttpException(error))
      }
  }

  override def updateFiles(project: Project, projectFiles: ProjectFiles)(
    implicit ec: ExecutionContext
  ): AsyncResult[List[SuccessResponseMessage]] = ???

  override def createRepository(localProject: LocalProject)(implicit ec: ExecutionContext): AsyncResult[Project] =
    if (!localProject.active) {
      Future.failed(VersioningException.RepositoryException("Could not create a repository for deleted project."))
    } else {
      val createRepoUrl: String = s"${config.url}projects"
      val postProject = PostProject(name = localProject.projectId.value)
      httpClient
        .post[GitLabRepositoryResponse, PostProject](url = createRepoUrl, headers = config.token, payload = postProject)
        .map {
          case Response(_, SuccessResponseBody(gitLabResponse), _) =>
            Right(localProject.toProject(gitLabResponse.id))
          case Response(statusCode, FailureResponseBody(error), _) =>
            Left {
              VersioningException.RepositoryException {
                s"The repository was not created. Response status: $statusCode; Response body [$error]"
              }
            }
        }
        .recover { case e: Throwable => Left(VersioningException.HttpException(e.getMessage)) }
    }

  override def getFiles(
    project: Project,
    version: Option[PipelineVersion]
  )(implicit ec: ExecutionContext): AsyncResult[List[ProjectFile]] = {
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

  override def getProjectVersions(project: Project)(implicit ec: ExecutionContext): AsyncResult[Seq[GitLabVersion]] = {
    val versionsListUrl: String = s"${config.url}projects/${project.repositoryId.value}/repository/tags"
    httpClient
      .get[Seq[GitLabVersion]](url = versionsListUrl, headers = config.token)
      .map {
        case Response(_, SuccessResponseBody(versionSeq), _) =>
          Right(versionSeq)
        case Response(_, FailureResponseBody(error), _) =>
          Left(
            VersioningException.ProjectException(
              s"Could not take versions. ResponseBody: $error"
            )
          )
      }
      .recover { case e: Throwable => Left(VersioningException.HttpException("recover " + e.getMessage)) }
  }

  override def getFileCommits(project: Project, path: Path)(
    implicit ec: ExecutionContext
  ): AsyncResult[Seq[FileCommit]] = {
    val urlEncoder = URLEncoderUtils.encode(path.toString)
    val commitsUrl: String =
      s"${config.url}projects/${project.repositoryId.value}/repository/files/$urlEncoder"
    httpClient.get[List[FileCommit]](url = commitsUrl, headers = config.token).map {
      case Response(_, SuccessResponseBody(commitsSeq), _) => Right(commitsSeq)
      case Response(_, FailureResponseBody(error), _) =>
        Left(
          VersioningException.FileException(
            s"Could not take the file commits. ResponseBody: $error"
          )
        )
    }
  }

  override def getFileVersions(project: Project, path: Path)(
    implicit ec: ExecutionContext
  ): AsyncResult[Seq[GitLabVersion]] = {
    val projectVersionsF = getProjectVersions(project)
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
            if tagFile.commitId == tagProject.commit.id
          } yield tagProject)
        case (_, Left(exception)) => Left(VersioningException.FileException(exception.getMessage))
        case (Left(exception), _) => Left(VersioningException.FileException(exception.getMessage))
      }
    }
  }

  override def getFilesVersions(project: Project, path: Path)(
    implicit ec: ExecutionContext
  ): AsyncResult[List[GitLabVersion]] = ???

  override def getDefaultProjectVersion()(
    implicit ec: ExecutionContext
  ): PipelineVersion = PipelineVersion(config.defaultFileVersion)

  override def getFilesTree(project: Project, version: Option[PipelineVersion])(
    implicit ec: ExecutionContext
  ): AsyncResult[Seq[FileTree]] = {
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
            VersioningException.FileException(
              s"Could not take the files tree or parse json. Response status: $statusCode. ResponseBody: $error"
            )
          )
      }
      .recover { case e: Throwable => Left(VersioningException.HttpException(e.getMessage)) }

  }

  override def getFile(project: Project, path: Path, version: Option[PipelineVersion])(
    implicit ec: ExecutionContext
  ): AsyncResult[ProjectFile] = {
    val filePath: String = URLEncoderUtils.encode(path.toString)
    val fileVersion: String = version match {
      case Some(version) => version.name
      case None          => getDefaultProjectVersion().toString
    }

    httpClient
      .get[GitLabFileContent](
        s"${config.url}/projects/${project.repositoryId.value}/repository/files/$filePath",
        Map("ref" -> fileVersion),
        config.token
      )
      .map {
        case Response(HttpStatusCodes.OK, SuccessResponseBody(gitLabFile), _) =>
          val content = decodeBase64(gitLabFile.content)
          Right(ProjectFile(path, ProjectFileContent(content)))
        case Response(HttpStatusCodes.OK, FailureResponseBody(error), _) =>
          Left(
            VersioningException.HttpException(
              s"Could not take Project File. Response status: ${HttpStatusCodes.OK}. ResponseBody: $error"
            )
          )
        case Response(responseStatus, _, _) =>
          Left(VersioningException.HttpException(s"Exception. Response status: $responseStatus"))
      }
      .recover { case e: Throwable => Left(VersioningException.HttpException(e.getMessage)) }
  }

  private def decodeBase64(str: String): String = new String(java.util.Base64.getDecoder.decode(str))
}
