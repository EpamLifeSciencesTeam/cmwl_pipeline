package cromwell.pipeline.datastorage.dao.repository

import cromwell.pipeline.datastorage.dto.{ ProjectId, Run }
import cromwell.pipeline.model.wrapper.{ RunId, UserId }

import scala.collection.mutable
import scala.concurrent.Future

class RunRepositoryTestImpl extends RunRepository {

  private val runs: mutable.Map[RunId, Run] = mutable.Map.empty

  def getRunByIdAndUser(runId: RunId, userId: UserId): Future[Option[Run]] = {
    val run = for {
      (id, entity) <- runs if id == runId && entity.userId == userId
    } yield entity
    Future.successful(run.headOption)
  }

  def getRunsByProject(projectId: ProjectId): Future[Seq[Run]] = {
    val runsByProject = runs.values.filter(_.projectId == projectId).toSeq
    Future.successful(runsByProject)
  }

  def deleteRunById(runId: RunId): Future[Int] = {
    runs -= runId
    Future.successful(0)
  }

  def addRun(run: Run): Future[RunId] = {
    runs += (run.runId -> run)
    Future.successful(run.runId)
  }

  def updateRun(updatedRun: Run): Future[Int] = {
    if (runs.contains(updatedRun.runId)) runs += (updatedRun.runId -> updatedRun)
    Future.successful(0)
  }

}

object RunRepositoryTestImpl {

  def apply(runs: Run*): RunRepositoryTestImpl = {
    val runRepositoryTestImpl = new RunRepositoryTestImpl
    runs.foreach(runRepositoryTestImpl.addRun)
    runRepositoryTestImpl
  }

}
