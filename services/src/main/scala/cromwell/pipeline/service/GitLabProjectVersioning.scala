package cromwell.pipeline.service

import java.net.URLEncoder
import java.nio.file.Path

import cromwell.pipeline.datastorage.dto.Project._
import cromwell.pipeline.datastorage.dto.File.UpdateFileRequest
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.utils.{ GitLabConfig, HttpStatusCodes }

import scala.concurrent.{ ExecutionContext, Future }

class GitLabProjectVersioning(httpClient: HttpClient, config: GitLabConfig)
    extends ProjectVersioning[VersioningException] {

  private def getLastProjectVersion(project: Project)(implicit ec: ExecutionContext): Future[Option[PipelineVersion]] =
    getProjectVersions(project).flatMap {
      case Left(error)                      => Future.failed(error)
      case Right(Seq(_))                    => Future.successful(None)
      case Right(Seq(v: GitLabVersion, _*)) => Future.successful(Some(v.name))
      case Right(Seq())                     => Future.successful(Some(PipelineVersion(config.defaultFileVersion)))
    }

  private def getNewProjectVersion(
    optionProjectVersion: Option[PipelineVersion],
    optionUserVersion: Option[PipelineVersion]
  ): Either[VersioningException, PipelineVersion] =
    (optionProjectVersion, optionUserVersion) match {
      case (Some(projectVersion), Some(userVersion)) =>
        if (projectVersion >= userVersion)
          Left(
            VersioningException.ProjectException(
              s"Your version $userVersion is out of date. Current version of project: $projectVersion"
            )
          )
        else Right(userVersion)
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
    repositoryId: Repository,
    version: PipelineVersion,
    responseBody: SuccessResponseMessage
  )(
    implicit ec: ExecutionContext
  ): AsyncResult[SuccessResponseMessage] =
    createTag(repositoryId, version).map {
      case Right(_)        => Right(responseBody)
      case Left(exception) => Left(exception)
    }

  override def updateFile(project: Project, projectFile: ProjectFile, userVersion: Option[PipelineVersion])(
    implicit ec: ExecutionContext
  ): AsyncResult[SuccessResponseMessage] = {
    val path = URLEncoder.encode(projectFile.path.toString, "UTF-8")
    val repositoryId: Repository =
      project.repository.getOrElse(
        throw VersioningException.RepositoryException(s"No repository for project: $project")
      )
    val fileUrl = s"${config.url}projects/${repositoryId.value}/repository/files/$path"

    getLastProjectVersion(project).map(projectVersion => getNewProjectVersion(projectVersion, userVersion)).flatMap {
      case Left(error) =>
        Future.successful(Left(error))
      case Right(newVersion) =>
        val payload = UpdateFileRequest(projectFile.content, newVersion.toString, config.defaultBranch)
        httpClient
          .put[SuccessResponseMessage, UpdateFileRequest](fileUrl, payload = payload, headers = config.token)
          .flatMap {
            case Response(_, SuccessResponseBody(body), _) =>
              handleCreateTag(repositoryId, newVersion, body)
            case _ =>
              httpClient
                .post[SuccessResponseMessage, UpdateFileRequest](fileUrl, payload = payload, headers = config.token)
                .flatMap {
                  case Response(_, SuccessResponseBody(body), _) =>
                    handleCreateTag(repositoryId, newVersion, body)
                  case Response(_, FailureResponseBody(body), _) =>
                    Future.successful(Left(VersioningException.HttpException(body)))
                }
          }
    }
  }

  private def createTag(projectId: Repository, version: PipelineVersion)(
    implicit ec: ExecutionContext
  ): AsyncResult[SuccessResponseMessage] = {
    val tagUrl = s"${config.url}projects/${projectId.value}/repository/tags"
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

  override def createRepository(project: Project)(implicit ec: ExecutionContext): AsyncResult[Project] =
    if (!project.active)
      Future.failed(VersioningException.RepositoryException("Could not create a repository for deleted project."))
    else {
      val createRepoUrl: String = s"${config.url}projects"
      val postProject = PostProject(name = project.name)
      httpClient
        .post[RepositoryId, PostProject](url = createRepoUrl, headers = config.token, payload = postProject)
        .map {
          case Response(_, SuccessResponseBody(gitLabProject), _) =>
            Right(project.withRepository(Some(s"${config.idPath}${gitLabProject.id}")))
          case Response(statusCode, FailureResponseBody(_), _) =>
            Left(
              VersioningException.RepositoryException(s"The repository was not created. Response status: ${statusCode}")
            )
        }
        .recover { case e: Throwable => Left(VersioningException.HttpException(e.getMessage)) }
    }

  override def getFiles(project: Project, path: Path)(implicit ec: ExecutionContext): AsyncResult[List[String]] = ???

  override def getProjectVersions(project: Project)(implicit ec: ExecutionContext): AsyncResult[Seq[GitLabVersion]] = {
    val versionsListUrl: String = s"${config.url}projects/${project.repository.get.value}/repository/tags"
    httpClient
      .get[Seq[GitLabVersion]](url = versionsListUrl, headers = config.token)
      .map {
        case Response(_, SuccessResponseBody(versionSeq), _) =>
          Right(versionSeq)
        case Response(_, FailureResponseBody(error), _) =>
          Left(
            VersioningException.ProjectException(
              s"Could not take versions. ResponseBody: ${error}"
            )
          )
      }
      .recover { case e: Throwable => Left(VersioningException.HttpException("recover " + e.getMessage)) }
  }

  override def getFileCommits(project: Project, path: Path)(
    implicit ec: ExecutionContext
  ): AsyncResult[Seq[FileCommit]] = {
    val urlEncoder = URLEncoder.encode(path.toString, "UTF-8")
    val commitsUrl: String =
      s"${config.url}projects/${project.repository.get.value}/repository/files/${urlEncoder}"
    httpClient.get[List[FileCommit]](url = commitsUrl, headers = config.token).map {
      case Response(_, SuccessResponseBody(commitsSeq), _) => Right(commitsSeq)
      case Response(_, FailureResponseBody(error), _) =>
        Left(
          VersioningException.FileException(
            s"Could not take the file commits. ResponseBody: ${error}"
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

  override def getFilesTree(project: Project, version: Option[PipelineVersion])(
    implicit ec: ExecutionContext
  ): AsyncResult[Seq[FileTree]] = {
    val versionId: Map[String, String] = version match {
      case Some(version) => Map("ref" -> version.name, "recursive" -> "true")
      case None          => Map()
    }
    val filesTreeUrl: String =
      s"${config.url}projects/${project.repository.get.value}/repository/tree"

    httpClient
      .get[List[FileTree]](url = filesTreeUrl, params = versionId, headers = config.token)
      .map {
        case Response(_, SuccessResponseBody(fileTreeSeq), _) => Right(fileTreeSeq)
        case Response(statusCode, FailureResponseBody(error), _) =>
          Left(
            VersioningException.FileException(
              s"Could not take the files tree or parse json. Response status: ${statusCode}. ResponseBody: ${error}"
            )
          )
      }
      .recover { case e: Throwable => Left(VersioningException.HttpException(e.getMessage)) }

  }

  override def getFile(project: Project, path: Path, version: Option[PipelineVersion])(
    implicit ec: ExecutionContext
  ): AsyncResult[ProjectFile] = {
    val filePath: String = URLEncoder.encode(path.toString, "UTF-8")
    val fileVersion: String = version match {
      case Some(version) => version.name
      case None          => config.defaultFileVersion
    }

    httpClient
      .get[ProjectFileContent](
        s"${config.url}/projects/${project.repository}/repository/files/$filePath/raw",
        Map("ref" -> fileVersion),
        config.token
      )
      .map {
        case Response(HttpStatusCodes.OK, SuccessResponseBody(projFileContent), _) =>
          Right(ProjectFile(path, projFileContent))
        case Response(HttpStatusCodes.OK, FailureResponseBody(error), _) =>
          Left(
            VersioningException.HttpException(
              s"Could not take Project File. Response status: ${HttpStatusCodes.OK}. ResponseBody: ${error}"
            )
          )
        case Response(responseStatus, _, _) =>
          Left(VersioningException.HttpException(s"Exception. Response status: ${responseStatus}"))
      }
      .recover { case e: Throwable => Left(VersioningException.HttpException(e.getMessage)) }
  }

  override def deleteFile(
    project: Project,
    path: Path,
    branchName: String = config.defaultBranch,
    commitMessage: String
  )(
    implicit ec: ExecutionContext
  ): AsyncResult[String] = {

    val filePath: String = URLEncoder.encode(path.toString, "UTF-8")
    val deleteMessage: String = s"$filePath file has been deleted from $branchName"

    httpClient
      .delete[String](
        s"${config.url}/projects/${project.repository}/repository/files/$filePath/raw",
        config.token
      )
      .map { resp =>
        resp.status match {
          case HttpStatusCodes.OK => Right(deleteMessage)
          case _                  => Left(VersioningException.HttpException(s"Exception. Response status: ${resp.status}"))
        }
      }
      .recover { case e: Throwable => Left(VersioningException.HttpException(e.getMessage)) }
  }
}
