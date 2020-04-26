package cromwell.pipeline.datastorage.dao.repository.utils

import java.util.UUID

import cromwell.pipeline.datastorage.dto._

object TestProjectUtils {

  private def randomUuidStr: String = UUID.randomUUID().toString
  def getDummyProjectId: ProjectId = ProjectId(randomUuidStr)
  def getDummyProject(
    projectId: ProjectId = getDummyProjectId,
    ownerId: UserId = TestUserUtils.getDummyUserId,
    name: String = s"project-$randomUuidStr",
    repository: String = s"repo-$randomUuidStr",
    active: Boolean = true,
    visibility: Visibility = Private
  ): Project = Project(projectId, ownerId, name, repository, active, visibility)
  def getDummyCommit(
    id: String = s"id-$randomUuidStr",
    shortId: String = s"shortID-$randomUuidStr",
    title: String = s"title-$randomUuidStr",
    message: String = s"message-$randomUuidStr"
  ): Commit = Commit(id, shortId, title, message)
  def getDummyVersion(
    name: String = s"name-$randomUuidStr",
    message: String = s"message-$randomUuidStr",
    target: String = s"target-$randomUuidStr",
    commit: Commit = getDummyCommit()
  ): Version = Version(name, message, target, commit)
}
