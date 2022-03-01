package cromwell.pipeline.datastorage.dto

import cromwell.pipeline.model.wrapper.{ ProjectId, UserId }

final case class CromwellInput(
  projectId: ProjectId,
  userId: UserId,
  projectVersion: PipelineVersion,
  files: List[ProjectFile],
  wdlParams: WdlParams
)
