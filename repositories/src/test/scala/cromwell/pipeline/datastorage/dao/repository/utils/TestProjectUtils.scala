package cromwell.pipeline.datastorage.dao.repository.utils

import java.util.UUID

import cromwell.pipeline.datastorage.dto._

object TestProjectUtils {

  private def randomUuidStr: String = UUID.randomUUID().toString
  def getDummyProjectId: ProjectId = ProjectId(randomUuidStr)
  def getDummyProject(
    projectId: ProjectId = getDummyProjectId,
    ownerId: UserId = TestUserUtils.getDummyUserId,
    name: String = s"project-" + randomUuidStr,
    repository: String = s"repo-" + randomUuidStr,
    visibility: Visibility = Private,
    active: Boolean = true
  ): Project = Project(projectId, ownerId, name, repository, visibility, active)
}
