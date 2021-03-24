package cromwell.pipeline.datastorage.dao.repository.utils

import java.time.Instant
import java.util.UUID

import cromwell.pipeline.datastorage.dto.{ Created, ProjectId, Run, Status }
import cromwell.pipeline.model.wrapper.{ RunId, UserId }

object TestRunUtils {

  private def randomUuidStr: String = UUID.randomUUID().toString
  def getDummyProjectId: ProjectId = ProjectId(randomUuidStr)
  def getDummyRunId: RunId = RunId.random
  def getDummyTimeStart: Instant = Instant.now()
  def getDummyTimeEnd(isEmpty: Boolean): Option[Instant] =
    if (isEmpty) None else Some(Instant.now())
  def getDummyCmwlWorkflowId(isEmpty: Boolean): Option[String] =
    if (isEmpty) None else Some(s"cmwlWorkflowId-$randomUuidStr")
  def getDummyRun(
    runId: RunId = getDummyRunId,
    projectId: ProjectId = getDummyProjectId,
    projectVersion: String = s"version-$randomUuidStr",
    status: Status = Created,
    timeStart: Instant = getDummyTimeStart,
    timeEnd: Option[Instant] = getDummyTimeEnd(true),
    userId: UserId = TestUserUtils.getDummyUserId,
    results: String = s"results-$randomUuidStr",
    cmwlWorkflowId: Option[String] = getDummyCmwlWorkflowId(true)
  ): Run = Run(runId, projectId, projectVersion, status, timeStart, timeEnd, userId, results, cmwlWorkflowId)
}
