package cromwell.pipeline.datastorage.dao.repository.utils

import java.util.UUID

import cromwell.pipeline.datastorage.dto.{ Project, ProjectId, UserId }

object TestProjectUtils {
  val userUuid: String = UUID.randomUUID().toString
  val projectUuid: String = UUID.randomUUID().toString

  def getDummyProject(
    projectId: ProjectId = ProjectId(projectUuid),
    ownerId: UserId = UserId(userUuid),
    name: String = s"project-$projectUuid",
    repository: String = s"repo-$projectUuid",
    active: Boolean = true
  ): Project =
    Project(projectId, ownerId, name, repository, active)
}
