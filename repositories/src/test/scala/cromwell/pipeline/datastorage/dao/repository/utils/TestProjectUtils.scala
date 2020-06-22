package cromwell.pipeline.datastorage.dao.repository.utils

import java.util.UUID

import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.UserId

import scala.util.Random

object TestProjectUtils {

  private def randomInt(range: Int): Int = Random.nextInt(range)
  private def randomUuidStr: String = UUID.randomUUID().toString
  def getDummyProjectId: ProjectId = ProjectId(randomUuidStr)
  def getDummyRepository: Repository = Repository(s"repo-$randomUuidStr")
  def getDummyProject(
    projectId: ProjectId = getDummyProjectId,
    ownerId: UserId = TestUserUtils.getDummyUserId,
    name: String = s"project-$randomUuidStr",
    repository: Option[Repository] = Some(getDummyRepository),
    active: Boolean = true,
    visibility: Visibility = Private
  ): Project = Project(projectId, ownerId, name, active, repository, visibility)
  def getDummyCommit(id: String = randomUuidStr): Commit = Commit(id)
  def getDummyPipeLineVersion(
    v1: Int = 1 + randomInt(12),
    v2: Int = 1 + randomInt(12),
    v3: Int = 1 + randomInt(12)
  ): PipelineVersion =
    PipelineVersion(s"v$v1.$v2.$v3")
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
