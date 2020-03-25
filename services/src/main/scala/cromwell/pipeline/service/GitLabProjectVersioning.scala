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

  override def createRepository(project: Project)(implicit ec: ExecutionContext): AsyncResult[Project] =
    if (!project.active)
      Future.failed(VersioningException("Could not create a repository for deleted project."))
    else {
      val createRepoUrl: String = s"${config.url}projects"
      val responseFuture =
        httpClient.post(url = createRepoUrl, headers = config.token, payload = createRepositoryBody(project))
      responseFuture
        .map(
          resp =>
            if (resp.status != StatusCodes.Created.intValue)
              Left(VersioningException(s"The repository was not created. Response status: ${resp.status}"))
            else Right(updateProject(project))
        )
        .recover { case e: Throwable => Left(VersioningException(e.getMessage)) }
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

  private def createRepositoryBody(project: Project): String = {
    val name: String = project.ownerId.value
    val path: String = project.projectId.value
    val visibility = "private"
    val jsValue = Json.toJson(Map("name" -> name, "path" -> path, "visibility" -> visibility))
    Json.stringify(jsValue)
  }

  private def updateProject(project: Project): Project =
    project.copy(repository = config.idPath + project.projectId.value)
}
