package cromwell.pipeline.datastorage.dao.entry

import java.time.Instant

import cromwell.pipeline.datastorage.Profile
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.{ RunId, UserId }
import slick.lifted.{ ForeignKeyQuery, ProvenShape }

trait RunEntry { this: Profile with UserEntry with ProjectEntry with CustomsWithEnumSupport with AliasesSupport =>
  import Implicits._
  import api._

  class RunTable(tag: Tag) extends Table[Run](tag, "run") {
    def * : ProvenShape[Run] =
      (runId, projectId, projectVersion, status, timeStart, timeEnd, userId, results, cmwlWorkflowId) <>
        ((Run.apply _).tupled, Run.unapply)

    def runId: Rep[RunId] = column[RunId]("run_id", O.PrimaryKey)
    def projectVersion: Rep[String] = column[String]("project_version")
    def status: Rep[Status] = column[Status]("status")
    def timeStart: Rep[Instant] = column[Instant]("time_start")
    def timeEnd: Rep[Option[Instant]] = column[Option[Instant]]("time_end")
    def results: Rep[String] = column[String]("results")
    def cmwlWorkflowId: Rep[Option[String]] = column[Option[String]]("cmwl_workflow_id")
    def projectId: Rep[ProjectId] = column[ProjectId]("project_id")
    def userId: Rep[UserId] = column[UserId]("user_id")
    def project: ForeignKeyQuery[ProjectTable, Project] = foreignKey("fk_run_project", projectId, projects)(_.projectId)
    def user: ForeignKeyQuery[UserTable, UserWithCredentials] = foreignKey("fk_run_user", userId, users)(_.userId)
  }

  val runs = TableQuery[RunTable]

  def getRunByIdAndUser(runId: RunId, userId: UserId): Query[RunTable, Run, Seq] =
    runs.filter(run => run.runId === runId && run.userId === userId).take(1)

  def getRunsByProject(projectId: ProjectId): Query[RunTable, Run, Seq] =
    runs.filter(run => run.projectId === projectId)

  def deleteRunById(runId: RunId): ActionResult[Int] = runs.filter(_.runId === runId).delete

  def updateRun(updatedRun: Run): ActionResult[Int] =
    runs
      .filter(_.runId === updatedRun.runId)
      .map(run => (run.status, run.timeStart, run.timeEnd, run.results, run.cmwlWorkflowId))
      .update(
        (
          updatedRun.status,
          updatedRun.timeStart,
          updatedRun.timeEnd,
          updatedRun.results,
          updatedRun.cmwlWorkflowId
        )
      )

  def addRun(run: Run): ActionResult[RunId] = runs.returning(runs.map(_.runId)) += run
}
