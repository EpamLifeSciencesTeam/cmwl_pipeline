package cromwell.pipeline.service
import java.nio.file.Path

import akka.http.scaladsl.model.StatusCodes
import cromwell.pipeline.datastorage.dto.{ Project, ProjectFile, Version }
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
            else Right(projectWithRepository(config.idPath + project.projectId))
        )
        .recover { case e: Throwable => Left(VersioningException(e.getMessage)) }
    }
  }
  override def getFiles(project: Project, path: Path)(implicit ec: ExecutionContext): AsyncResult[List[String]] = ???
  override def getProjectVersions(project: Project)(implicit ec: ExecutionContext): AsyncResult[Project] = ???
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
  ): AsyncResult[String] = ???
}
