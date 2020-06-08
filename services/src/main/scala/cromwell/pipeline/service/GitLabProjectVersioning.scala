package cromwell.pipeline.service

import java.net.URLEncoder
import java.nio.file.Path

import cromwell.pipeline.datastorage.dto.{ Project, ProjectFile, UpdateFileRequest, Version }
import cromwell.pipeline.datastorage.formatters.ProjectFormatters._
import cromwell.pipeline.utils.{ GitLabConfig, HttpStatusCodes }
import play.api.libs.json.{ JsError, JsResult, JsSuccess, Json }

import scala.concurrent.{ ExecutionContext, Future }

class GitLabProjectVersioning(httpClient: HttpClient, config: GitLabConfig)
    extends ProjectVersioning[VersioningException] {

  override def updateFile(project: Project, projectFile: ProjectFile, version: Option[Version])(
    implicit ec: ExecutionContext
  ): AsyncResult[String] = {
    val path = URLEncoder.encode(projectFile.path.toString, "UTF-8")
    val fileUrl = s"${config.url}projects/${project.repository}/repository/files/$path"
    val versionValue = version.map(_.name).getOrElse(config.defaultFileVersion)

    httpClient
      .put(
        fileUrl,
        payload = Json.stringify(
          Json.toJson(UpdateFileRequest(versionValue, projectFile.content, versionValue))
        ),
        headers = config.token
      )
      .flatMap {
        case Response(HttpStatusCodes.OK, _, _) =>
          Future.successful(Right("Success update file"))
        case Response(HttpStatusCodes.BadRequest, _, _) =>
          httpClient
            .post(
              fileUrl,
              payload = Json.stringify(
                Json.toJson(UpdateFileRequest(config.defaultFileVersion, projectFile.content, "Init commit"))
              ),
              headers = config.token
            )
            .map {
              case Response(HttpStatusCodes.OK, _, _) => Right("Create new file")
              case Response(_, body, _)               => Left(VersioningException(body))
            }
        case Response(_, body, _) => Future.successful(Left(VersioningException(body)))
      }
  }

  override def updateFiles(project: Project, projectFiles: ProjectFiles)(
    implicit ec: ExecutionContext
  ): AsyncResult[List[String]] = ???

  override def createRepository(project: Project)(implicit ec: ExecutionContext): AsyncResult[Project] =
    if (!project.active)
      Future.failed(VersioningException("Could not create a repository for deleted project."))
    else {
      val createRepoUrl: String = s"${config.url}projects"
      httpClient
        .post(url = createRepoUrl, headers = config.token, payload = Json.stringify(Json.toJson(project)))
        .map(
          resp =>
            if (resp.status != HttpStatusCodes.Created)
              Left(VersioningException(s"The repository was not created. Response status: ${resp.status}"))
            else Right(project.withRepository(Some(s"${config.idPath}${project.projectId.value}")))
        )
        .recover { case e: Throwable => Left(VersioningException(e.getMessage)) }
    }

  override def getFiles(project: Project, path: Path)(implicit ec: ExecutionContext): AsyncResult[List[String]] = ???

  override def getProjectVersions(project: Project)(implicit ec: ExecutionContext): AsyncResult[Seq[Version]] = {
    val versionsListUrl: String = s"${config.url}projects/${project.repository.get.value}/repository/tags"
    httpClient
      .get(url = versionsListUrl, headers = config.token)
      .map(
        resp =>
          if (resp.status != HttpStatusCodes.OK)
            Left(VersioningException(s"Could not take versions. Response status: ${resp.status}"))
          else {
            val parsedVersions: JsResult[Seq[Version]] = Json.parse(resp.body).validate[Seq[Version]]
            parsedVersions match {
              case JsSuccess(value, _) => Right(value)
              case JsError(errors)     => Left(VersioningException(s"Could not parse GitLab response. (errors: $errors)"))
            }
          }
      )
      .recover { case e: Throwable => Left(VersioningException(e.getMessage)) }
  }

  override def getFileVersions(project: Project, path: Path)(
    implicit ec: ExecutionContext
  ): AsyncResult[List[Version]] = ???

  override def getFilesVersions(project: Project, path: Path)(
    implicit ec: ExecutionContext
  ): AsyncResult[List[Version]] = ???

  override def getFileTree(project: Project, version: Option[Version])(
    implicit ec: ExecutionContext
  ): AsyncResult[List[String]] = ???

  override def getFile(project: Project, path: Path, version: Option[Version])(
    implicit ec: ExecutionContext
  ): AsyncResult[ProjectFile] = {
    val filePath: String = URLEncoder.encode(path.toString, "UTF-8")
    val fileVersion: String = version match {
      case Some(version) => version.name
      case None          => config.defaultFileVersion
    }

    httpClient
      .get(
        s"${config.url}/projects/${project.repository}/repository/files/$filePath/raw",
        Map("ref" -> fileVersion),
        config.token
      )
      .map(
        resp =>
          resp.status match {
            case HttpStatusCodes.OK => Right(ProjectFile(path, resp.body))
            case _                  => Left(VersioningException(s"Exception. Response status: ${resp.status}"))
          }
      )
      .recover { case e: Throwable => Left(VersioningException(e.getMessage)) }
  }
}
