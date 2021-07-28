package cromwell.pipeline.service
import cromwell.pipeline.datastorage.dto.{ Created, Run, RunCreateRequest, RunUpdateRequest }
import cromwell.pipeline.model.wrapper.{ RunId, UserId }

import java.time.Instant
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RunServiceTestImpl extends RunService {

  val runs: mutable.Map[RunId, Run] = mutable.Map.empty

  override def addRun(runCreateRequest: RunCreateRequest): Future[RunId] = {

    val newRun = Run(
      runId = RunId.random,
      projectId = runCreateRequest.projectId,
      projectVersion = runCreateRequest.projectVersion,
      status = Created,
      timeStart = Instant.now(),
      userId = runCreateRequest.userId,
      results = runCreateRequest.results
    )
    runs += (newRun.runId -> newRun)
    Future.successful(newRun.runId)
  }

  override def getRunByIdAndUser(runId: RunId, userId: UserId): Future[Option[Run]] = {
    val run = for {
      (id, entity) <- runs if id == runId && entity.userId == userId
    } yield entity
    Future.successful(run.headOption)
  }

  override def deleteRunById(runId: RunId, userId: UserId): Future[Int] = {
    runs -= runId
    Future.successful(0)
  }

  override def updateRun(request: RunUpdateRequest, userId: UserId): Future[Int] =
    getRunByIdAndUser(request.runId, userId).flatMap {
      case Some(run) =>
        val updatedRun = run.copy(
          status = request.status,
          timeStart = request.timeStart,
          timeEnd = request.timeEnd,
          results = request.results,
          cmwlWorkflowId = request.cmwlWorkflowId
        )
        runs += (updatedRun.runId -> updatedRun)
        Future.successful(0)

      case None => Future.failed(new RuntimeException("run with this id doesn't exist"))
    }
}
object RunServiceTestImpl {

  def apply(testRuns: Run*): RunServiceTestImpl = {
    val runServiceTestImpl = new RunServiceTestImpl
    testRuns.foreach(run => runServiceTestImpl.runs += (run.runId -> run))
    runServiceTestImpl
  }

}
