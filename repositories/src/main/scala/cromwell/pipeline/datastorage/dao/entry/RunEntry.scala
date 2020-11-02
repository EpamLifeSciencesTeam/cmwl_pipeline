package cromwell.pipeline.datastorage.dao.entry

import java.time.Instant

import cromwell.pipeline.datastorage.Profile
import cromwell.pipeline.datastorage.dao.ProjectEntry
import cromwell.pipeline.datastorage.dto.{ CustomsWithEnumSupport, ProjectId, Run, Status }
import cromwell.pipeline.model.wrapper.{ RunId, UserId }

trait RunEntry { this: Profile with UserEntry with ProjectEntry with CustomsWithEnumSupport =>
  import Implicits._
  import api._

  class RunTable(tag: Tag) extends Table[Run](tag, "run") {
    def * = (runId, projectId, projectVersion, status, timeStart, timeEnd.?, userId, results, cmwlWorkflowId.?) <>
      ((Run.apply _).tupled, Run.unapply)

    def runId = column[RunId]("run_id", O.PrimaryKey)

    def projectVersion = column[String]("project_version")
    def status = column[Status]("status")
    def timeStart = column[Instant]("time_start")
    def timeEnd = column[Instant]("time_end")

    def results = column[String]("results")

    def cmwlWorkflowId = column[String]("cmwl_workflow_id")

    def project = foreignKey("fk_run_project", projectId, projects)(_.projectId)

    def projectId = column[ProjectId]("project_id")

    def user = foreignKey("fk_run_user", userId, users)(_.userId)

    def userId = column[UserId]("user_id")
  }

  val runs = TableQuery[RunTable]

  def getRunById = Compiled { runId: Rep[RunId] =>
    runs.filter(_.runId === runId).take(1)
  }

  def deleteRunById(runId: RunId) = runs.filter(_.runId === runId).delete

  def updateRun(updatedRun: Run) =
    runs
      .filter(_.runId === updatedRun.runId)
      .map(run => (run.status, run.timeStart, run.timeEnd, run.results, run.cmwlWorkflowId))
      .update(
        (
          updatedRun.status,
          updatedRun.timeStart,
          updatedRun.timeEnd.get,
          updatedRun.results,
          updatedRun.cmwlWorkflowId.get
        )
      )

  def addRun(run: Run) = runs.returning(runs.map(_.runId)) += run
}
