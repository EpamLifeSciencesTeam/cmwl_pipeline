package cromwell.pipeline.service

import java.nio.file.Path

import akka.http.scaladsl.model.StatusCodes
import cromwell.pipeline.datastorage.dto.{ Commit, Project, ProjectFile, Version }
import cromwell.pipeline.utils.GitLabConfig
import play.api.libs.json.Json

import scala.concurrent.{ ExecutionContext, Future }

class GitLabProjectVersioning(httpClient: HttpClient, config: GitLabConfig)
    extends ProjectVersioning[VersioningException] {

  override def updateFile(project: Project, projectFile: ProjectFile)(
    implicit ec: ExecutionContext
  ): AsyncResult[String] = ???

  override def updateFiles(project: Project, projectFiles: ProjectFiles)(
    implicit ec: ExecutionContext
  ): AsyncResult[List[String]] = ???

  override def createRepository(project: Project)(implicit ec: ExecutionContext): AsyncResult[Project] = {
    def projectWithRepository(repositoryFullPath: String): Project = project.copy(repository = repositoryFullPath)
    if (!project.active)
      Future.failed(VersioningException("Could not create a repository for deleted project."))
    else {
      val createRepoUrl: String = s"${config.url}projects"
      httpClient
        .post(url = createRepoUrl, headers = config.token, payload = Json.stringify(Json.toJson(project)))
        .map(
          resp =>
            if (resp.status != StatusCodes.Created.intValue)
              Left(VersioningException(s"The repository was not created. Response status: ${resp.status}"))
            else Right(projectWithRepository(config.idPath + project.projectId.value))
        )
        .recover { case e: Throwable => Left(VersioningException(e.getMessage)) }
    }
  }

  override def getFiles(project: Project, path: Path)(implicit ec: ExecutionContext): AsyncResult[List[String]] = ???

  override def getProjectVersions(project: Project)(implicit ec: ExecutionContext): AsyncResult[Seq[Version]] =
    if (project.repository == null)
      Future.failed(VersioningException("There is no repository in this project"))
    else {
      val versionsListUrl: String = s"${config.url}/projects/${project.repository}/repository/tags"
      httpClient
        .get(url = versionsListUrl, headers = config.token)
        .map(
          resp =>
            if (resp.status != StatusCodes.OK.intValue)
              Left(VersioningException(s"Could not take versions. Response status: ${resp.status}"))
            else {
              val versionsBody = Json.parse(resp.body).validate[Seq[Version]]
              Right(versionsBody.get)
            }
        )
        .recover { case e: Throwable => Left(VersioningException(e.getMessage)) }
    }

  override def getFileCommits(project: Project, path: Path)(implicit ec: ExecutionContext): AsyncResult[Seq[Commit]] = {
    val commitsListUrl: String = s"${config.url}projects/${project.projectId}/repository/commits"
    httpClient
      .get(url = commitsListUrl, params = Map("path" -> path.toString), headers = config.token)
      .map(
        response =>
          if (response.status != StatusCodes.OK.intValue)
            Left(VersioningException(s"Could not take commits. Response status: ${response.status}"))
          else {
            val commitsBody = Json.parse(response.body).validate[Seq[Commit]]
            Right(commitsBody.get)
          }
      )
      .recover { case e: Throwable => Left(VersioningException(e.getMessage)) }
  }

  override def getFileVersions(project: Project, path: Path)(implicit ec: ExecutionContext): AsyncResult[Seq[Version]] =
    getProjectVersions(project).flatMap { eitherVersions =>
      getFileCommits(project, path).map { eitherCommits =>
        eitherCommits.flatMap { commits =>
          eitherVersions.map { versions =>
            versions.filter(version => commits.contains(version.commit))
          }
        }
      }
    }

  override def getFilesVersions(project: Project, path: Path)(
    implicit ec: ExecutionContext
  ): AsyncResult[List[Version]] = ???

  override def getFileTree(project: Project, version: Option[Version])(
    implicit ec: ExecutionContext
  ): AsyncResult[List[String]] = ???

  override def getFile(project: Project, path: Path, version: Option[Version])(
    implicit ec: ExecutionContext
  ): AsyncResult[String] = ???
}
