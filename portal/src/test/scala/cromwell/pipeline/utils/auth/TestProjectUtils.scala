package cromwell.pipeline.utils.auth

import java.util.UUID

import cromwell.pipeline.datastorage.dto.{ Project, ProjectId, UserId }

object TestProjectUtils {

  def getDummyProject(
    projectId: String = UUID.randomUUID().toString,
    userId: String = UUID.randomUUID().toString,
    name: String = "dummyProject",
    repository: String = "dummyRepository",
    active: Boolean = true
  ): Project =
    Project(
      ProjectId(projectId),
      UserId(userId),
      name,
      repository,
      active
    )

}
