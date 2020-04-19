package cromwell.pipeline.service
import java.net.URLEncoder
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
            else Right(updateProject(project))
          )
      )
  }

  override def getFiles(project: Project, path: Path): AsyncResult[List[String]] = ???
  override def getProjectVersions(project: Project): AsyncResult[Project] = ???
  override def getFileVersions(project: Project, path: Path, branch: String = "master"): AsyncResult[List[Version]] = {
    val projectId = project.projectId
    val filePath: String = URLEncoder.encode(path.toString, "UTF-8")
    val tags = httpClient.get(s"$URL/projects/$projectId/repository/", TOKEN)
    val fileCommits = httpClient.get(
      s"$URL/projects/$projectId/repository/commits",
      Map("path" -> filePath, "ref_name" -> branch),
      TOKEN
    )

  }
  override def getFileTree(project: Project, version: Option[Version]): AsyncResult[List[String]] = ???

  override def getFile(project: Project, path: Path, version: Option[Version]): AsyncResult[String] = {
    //    https://gitlab.example.com/api/v4/projects/13083/repository/files/app%2Fmodels%2Fkey%2Erb/raw?ref=master
    //    GET /projects/:id/repository/files/:file_path/raw

    val ownerId: String = project.ownerId.value
    val projectId: String = project.projectId.value
    val filePath: String = URLEncoder.encode(path.toString, "UTF-8")
    val fileVersion: String = version.map((el) => el.value).getOrElse("master")

    def responseFuture = httpClient.get(
      //      s"$URL/projects/$projectId/repository/files/$filePath",
      s"$URL/projects/$projectId/repository/files/$filePath/raw",
      Map("ref" -> fileVersion),
      TOKEN
    )

    responseFuture.flatMap(
      resp =>
        Future.successful(
          if (resp.status != 200) {
            Left(VersioningException(s"Exception. Response status: ${resp.status}"))
          } else {
            Right(resp.body)
          }
        )
    )
            else Right(projectWithRepository(s"${config.idPath}${project.projectId.value}"))
        )
        .recover { case e: Throwable => Left(VersioningException(e.getMessage)) }
    }
  }

  private def createRepositoryParams(project: Project): Map[String, String] = {
    val name: String = project.ownerId.value
    val path: String = project.projectId.value
    val visibility = "private"
    Map(("name", name), ("path", path), ("visibility", visibility))
  }

  private def updateProject(project: Project): Project =
    Project(project.projectId, project.ownerId, project.name, NAMESPACE + project.projectId.value, active = true)

}
