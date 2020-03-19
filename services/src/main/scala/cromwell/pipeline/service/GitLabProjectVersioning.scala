package cromwell.pipeline.service
import java.nio.file.Path

import com.typesafe.config.{ Config, ConfigFactory }
import cromwell.pipeline.datastorage.dto.{ Project, ProjectFile, Version }

import scala.concurrent.{ ExecutionContext, Future }

class GitLabProjectVersioning(httpClient: HttpClient)(
  implicit executionContext: ExecutionContext
) extends ProjectVersioning[VersioningException] {
  import GitLabConfig._

  override def updateFile(project: Project, projectFile: ProjectFile): AsyncResult[String] = ???
  override def updateFiles(project: Project, projectFiles: ProjectFiles): AsyncResult[List[String]] = ???

  override def createRepository(project: Project): AsyncResult[Project] = {
    def responseFuture = httpClient.post(URL + "projects", createRepositoryParams(project), TOKEN, "")
    if (!project.active)
      Future.failed(VersioningException("Could not create a repository for not an active project."))
    else
      responseFuture.flatMap(
        resp =>
          Future.successful(
            if (resp.status != CREATED)
              Left(VersioningException(s"The repository was not created. Response status: ${resp.status}"))
            else Right(updateProject(project))
          )
      )
  }

  override def getFiles(project: Project, path: Path): AsyncResult[List[String]] = ???
  override def getProjectVersions(project: Project): AsyncResult[Project] = ???
  override def getFileVersions(project: Project, path: Path): AsyncResult[List[Version]] = ???
  override def getFilesVersions(project: Project, path: Path): AsyncResult[List[Version]] = ???
  override def getFileTree(project: Project, version: Option[Version]): AsyncResult[List[String]] = ???
  override def getFile(project: Project, path: Path, version: Option[Version]): AsyncResult[String] = ???

  private def createRepositoryParams(project: Project): Map[String, String] = {
    val name: String = project.ownerId.value
    val path: String = project.projectId.value
    val visibility = "private"
    Map(("name", name), ("path", path), ("visibility", visibility))
  }

  private def updateProject(project: Project): Project =
    Project(project.projectId, project.ownerId, project.name, PATH + project.projectId.value, active = true)
}

object GitLabConfig {
  val config: Config = ConfigFactory.load()
  lazy val URL: String = config.getString("database.gitlab.url")
  lazy val PATH: String = config.getString("database.gitlab.path")
  lazy val TOKEN: Map[String, String] = Map("PRIVATE-TOKEN" -> config.getString("database.gitlab.token"))
  lazy val CREATED: Int = 201;
}
