package cromwell.pipeline.datastorage.dao.utils

import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId

import java.nio.file.Paths
import java.util.UUID
import scala.util.Random

object TestProjectUtils {

  private val defaultRange: Int = 100
  private def randomInt(range: Int = defaultRange): Int = Random.nextInt(range)
  private def randomUuidStr: String = UUID.randomUUID().toString
  def getDummyProjectId: ProjectId = ProjectId(randomUuidStr)
  def getDummyRepositoryId: RepositoryId = RepositoryId(randomInt())
  def getDummyProject(
    projectId: ProjectId = getDummyProjectId,
    ownerId: UserId = TestUserUtils.getDummyUserId,
    name: String = s"project-$randomUuidStr",
    repository: RepositoryId = getDummyRepositoryId,
    active: Boolean = true,
    version: PipelineVersion = getDummyPipeLineVersion(),
    visibility: Visibility = Private
  ): Project = Project(projectId, ownerId, name, active, repository, version, visibility)
  def getDummyProjectConfiguration(
    projectId: ProjectId = getDummyProjectId,
    projectConfigurationId: ProjectConfigurationId = ProjectConfigurationId.randomId,
    projectFileConfiguration: ProjectFileConfiguration =
      ProjectFileConfiguration(Paths.get("/home/file"), List(FileParameter("nodeName", StringTyped(Some("String")))))
  ): ProjectConfiguration = {
    val projectConfiguration: ProjectConfiguration = ProjectConfiguration(
      projectConfigurationId,
      projectId,
      active = true,
      List(projectFileConfiguration),
      ProjectConfigurationVersion.defaultVersion
    )
    projectConfiguration
  }
  def getDummyLocalProject(
    projectId: ProjectId = getDummyProjectId,
    ownerId: UserId = TestUserUtils.getDummyUserId,
    name: String = s"project-$randomUuidStr",
    active: Boolean = true,
    version: PipelineVersion = getDummyPipeLineVersion(),
    visibility: Visibility = Private
  ): LocalProject =
    LocalProject(projectId, ownerId, name, active, version, visibility)
  def getDummyCommit(id: String = randomUuidStr): Commit = Commit(id)
  def getDummyPipeLineVersion(
    v1: Int = 1 + randomInt(),
    v2: Int = 1 + randomInt(),
    v3: Int = 1 + randomInt()
  ): PipelineVersion =
    PipelineVersion(s"v$v1.$v2.$v3")
  def getDummyGitLabRepositoryResponse(
    repositoryId: RepositoryId = getDummyRepositoryId
  ): GitLabRepositoryResponse = GitLabRepositoryResponse(repositoryId)
  def getDummyGitLabVersion(
    version: PipelineVersion = getDummyPipeLineVersion(),
    message: String = s"message-$randomUuidStr",
    target: String = s"target-$randomUuidStr",
    commit: Commit = getDummyCommit()
  ): GitLabVersion = GitLabVersion(version, message, target, commit)
  def getDummyFileCommit(
    commitId: String = s"$randomUuidStr"
  ): FileCommit = FileCommit(commitId)
  def getDummyFileTree(
    id: String = s"id-$randomUuidStr",
    name: String = s"name-$randomUuidStr",
    path: String = s"path-$randomUuidStr",
    mode: String = s"mode-$randomUuidStr"
  ): FileTree = FileTree(id, name, path, mode)
}
