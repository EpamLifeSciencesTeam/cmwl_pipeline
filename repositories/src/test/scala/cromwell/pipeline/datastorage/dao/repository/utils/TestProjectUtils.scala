package cromwell.pipeline.datastorage.dao.repository.utils

import java.util.UUID

import cromwell.pipeline.datastorage.dto._

object TestProjectUtils {

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
  def getDummyVersion(
    name: String = s"name-$randomUuidStr",
    message: String = s"message-$randomUuidStr",
    target: String = s"target-$randomUuidStr",
    commit: Commit = getDummyCommit()
  ): Version = Version(name, message, target, commit)
}
