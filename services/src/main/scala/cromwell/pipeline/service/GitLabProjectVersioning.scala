package cromwell.pipeline.service
import java.net.URLEncoder
import java.nio.file.Path

import cromwell.pipeline.datastorage.dto.{Project, ProjectFile, Version}

import scala.concurrent.{ExecutionContext, Future}

class GitLabProjectVersioning(httpClient: HttpClient)(
  implicit executionContext: ExecutionContext
) extends ProjectVersioning[VersioningException] {

  lazy val URL: String = "https://gitlab.com/api/v4"
  lazy val NAMESPACE
    : String = "AdminLogin%2F" //to access repo in GitLab use this combo: NAMESPACE + project.projectId.value
  lazy val TOKEN: Map[String, String] = Map("PRIVATE-TOKEN" -> "s1ui8JcCCE-zhpyWDDmU") //access_token must be here


  override def updateFile(project: Project, projectFile: ProjectFile): AsyncResult[String] = ???
  override def updateFiles(project: Project, projectFiles: ProjectFiles): AsyncResult[List[String]] = ???

  override def createRepository(project: Project): AsyncResult[Project] = {
    def responseFuture = httpClient.post(URL + "/projects", createRepositoryParams(project), TOKEN, "")
    if (project.active)
      Future.failed(VersioningException("The repository is already active, creation failed."))
    else
      responseFuture.flatMap(
        resp =>
          Future.successful(
            if (resp.status != 201)
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

  override def getFile(project: Project, path: Path, version: Option[Version]): AsyncResult[String] = {
  //    https://gitlab.example.com/api/v4/projects/13083/repository/files/app%2Fmodels%2Fkey%2Erb/raw?ref=master
//    GET /projects/:id/repository/files/:file_path/raw

    val ownerId: String = project.ownerId.value
    val projectId: String = project.projectId.value
    val filePath: String = URLEncoder.encode(path.toString, "UTF-8")
    val fileVersion: String = version.map((el)=> el.value).getOrElse("master")

    def responseFuture = httpClient.get(
//      s"$URL/projects/$projectId/repository/files/$filePath",
      s"$URL/projects/$projectId/repository/files/$filePath/raw",
      Map("ref" -> fileVersion),
      TOKEN
    )

//      Map instead of flatMap
    responseFuture.flatMap(
      resp => Future.successful(
        if (resp.status == 200) {
          Right(resp.body)
        } else {
          Left(VersioningException(s"Exception. Response status: ${resp.status}"))
        }
      )
    )
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
