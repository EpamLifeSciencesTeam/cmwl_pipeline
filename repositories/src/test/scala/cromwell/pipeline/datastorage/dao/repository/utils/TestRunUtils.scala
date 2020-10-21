package cromwell.pipeline.datastorage.dao.repository.utils

import java.time.Instant
import java.util.{ Date, UUID }

import cromwell.pipeline.datastorage.dto.{ ProjectId, Run, Status, Submitted }
import cromwell.pipeline.model.wrapper.{ RunId, UserId }

object TestRunUtils {

  private def randomUuidStr: String = UUID.randomUUID().toString
  def getDummyProjectId: ProjectId = ProjectId(randomUuidStr)
  def getDummyRunId: RunId = RunId.random
  def getDummyTimeStart: Instant = new Date().toInstant
  def getDummyTimeEnd(isEmpty: Boolean): Option[Instant] =
    if (isEmpty) None else Some(new Date().toInstant)
  def getDummyCmwlWorkflowId(isEmpty: Boolean): Option[String] =
    if (isEmpty) None else Some(s"cmwlWorkflowId-$randomUuidStr")
  def getDummyRun(
    runId: RunId = getDummyRunId,
    projectId: ProjectId = getDummyProjectId,
    projectVersion: String = s"version-$randomUuidStr",
    status: Status = Submitted,
    timeStart: Instant = getDummyTimeStart,
    timeEnd: Option[Instant] = getDummyTimeEnd(true),
    userId: UserId = TestUserUtils.getDummyUserId,
    results: String = s"results-$randomUuidStr",
    cmwlWorkflowId: Option[String] = getDummyCmwlWorkflowId(true)
  ): Run = Run(runId, projectId, projectVersion, status, timeStart, timeEnd, userId, results, cmwlWorkflowId)
}
