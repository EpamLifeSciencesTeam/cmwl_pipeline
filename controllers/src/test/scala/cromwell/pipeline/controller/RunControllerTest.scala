package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dao.utils.{ TestProjectUtils, TestRunUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.datastorage.dto.{ Run, RunCreateRequest, RunUpdateRequest }
import cromwell.pipeline.model.wrapper.RunId
import cromwell.pipeline.service.RunService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class RunControllerTest
    extends AsyncWordSpec
    with Matchers
    with ScalatestRouteTest
    with MockitoSugar
    with BeforeAndAfterAll {
  import PlayJsonSupport._

  private val runService = mock[RunService]
  private val runController = new RunController(runService)
  private val accessToken = AccessTokenContent(TestUserUtils.getDummyUserId)

  "RunController" when {

    "gets run by id" should {

      "returns the run entity" taggedAs Controller in {
        val dummyRun: Run = TestRunUtils.getDummyRun()
        val runId = dummyRun.runId
        val projectId = dummyRun.projectId
        val runRespOption: Option[Run] = Option(dummyRun)

        when(runService.getRunByIdAndUser(runId, projectId, accessToken.userId))
          .thenReturn(Future.successful(runRespOption))

        Get(s"/projects/${projectId.value}/runs/$runId") ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Option[Run]] shouldEqual runRespOption
        }
      }

      "returns the internal server error if service fails" taggedAs Controller in {
        val dummyRun: Run = TestRunUtils.getDummyRun()
        val runId = dummyRun.runId
        val projectId = dummyRun.projectId
        when(runService.getRunByIdAndUser(runId, projectId, accessToken.userId))
          .thenReturn(Future.failed(new RuntimeException("something went wrong")))

        Get(s"/projects/${projectId.value}/runs/$runId") ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }

      "returns NotFound if the run id is not valid" taggedAs Controller in {
        val projectId = TestRunUtils.getDummyProjectId
        val invalidRunId = "123"

        Get(s"/projects/${projectId.value}/runs/$invalidRunId") ~> Route.seal(runController.route(accessToken)) ~> check {
          status shouldBe StatusCodes.NotFound
        }
      }
    }

    "get runs by project" should {

      "returns all runs of a project" taggedAs Controller in {
        val dummyRun1 = TestRunUtils.getDummyRun()
        val projectId = dummyRun1.projectId
        val dummyRun2: Run = TestRunUtils.getDummyRun(projectId = projectId)
        val runRespSeq: Seq[Run] = Seq(dummyRun1, dummyRun2)

        when(runService.getRunsByProject(projectId, accessToken.userId)).thenReturn(Future.successful(runRespSeq))

        Get(s"/projects/${projectId.value}/runs") ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Seq[Run]] shouldEqual runRespSeq
        }
      }

      "returns the internal server error if service fails" taggedAs Controller in {
        val dummyRun: Run = TestRunUtils.getDummyRun()
        val projectId = dummyRun.projectId
        when(runService.getRunsByProject(projectId, accessToken.userId))
          .thenReturn(Future.failed(new RuntimeException("something went wrong")))

        Get(s"/projects/${projectId.value}/runs") ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }

    "deleteRunById" should {
      "returns run 1 if the entity was deleted" taggedAs Controller in {
        val dummyRun: Run = TestRunUtils.getDummyRun()
        val runId = dummyRun.runId
        val projectId = dummyRun.projectId

        when(runService.deleteRunById(runId, projectId, accessToken.userId)).thenReturn(Future.successful(1))

        Delete(s"/projects/${projectId.value}/runs/$runId") ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
        }
      }
      "returns server error if run delete was failed" taggedAs Controller in {
        val runId = TestRunUtils.getDummyRunId
        val projectId = TestProjectUtils.getDummyProjectId
        when(runService.deleteRunById(runId, projectId, accessToken.userId))
          .thenReturn(Future.failed(new RuntimeException("something went wrong")))

        Delete(s"/projects/${projectId.value}/runs/$runId") ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
      "returns InternalServerError status if run id doesn't match" in {
        val runId = TestRunUtils.getDummyRunId
        val projectId = TestProjectUtils.getDummyProjectId
        when(runService.deleteRunById(runId, projectId, accessToken.userId))
          .thenReturn(Future.failed(new RuntimeException("Something wrong.")))

        Delete(s"/projects/${projectId.value}/runs/$runId") ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }

    "updates run entity" should {
      "returns NoContent status if run was updated" in {
        val dummyRun: Run = TestRunUtils.getDummyRun()
        val runId = dummyRun.runId
        val projectId = dummyRun.projectId
        val request = RunUpdateRequest(
          dummyRun.status,
          dummyRun.timeStart,
          dummyRun.timeEnd,
          dummyRun.results,
          dummyRun.cmwlWorkflowId
        )
        when(runService.updateRun(runId, request, projectId, accessToken.userId)).thenReturn(Future.successful(1))

        Put(s"/projects/${projectId.value}/runs/$runId", request) ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NoContent
        }
      }

      "returns NoContent status if run entity was updated without time end" in {
        val runId = TestRunUtils.getDummyRunId
        val dummyRun: Run = TestRunUtils.getDummyRun()
        val projectId = dummyRun.projectId
        val request = RunUpdateRequest(
          dummyRun.status,
          dummyRun.timeStart,
          None,
          dummyRun.results,
          dummyRun.cmwlWorkflowId
        )
        when(runService.updateRun(runId, request, projectId, accessToken.userId)).thenReturn(Future.successful(1))

        Put(s"/projects/${projectId.value}/runs/$runId", request) ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NoContent
        }
      }

      "returns InternalServerError status if run id doesn't match" in {
        val runId = TestRunUtils.getDummyRunId
        val dummyRun: Run = TestRunUtils.getDummyRun()
        val projectId = dummyRun.projectId
        val request = RunUpdateRequest(
          dummyRun.status,
          dummyRun.timeStart,
          None,
          dummyRun.results,
          dummyRun.cmwlWorkflowId
        )

        when(runService.updateRun(runId, request, projectId, accessToken.userId))
          .thenReturn(Future.failed(new RuntimeException("something went wrong")))

        Put(s"/projects/${projectId.value}/runs/$runId", request) ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }

    "adds run entity" should {

      "returns the run id" taggedAs Controller in {
        val dummyRun: Run = TestRunUtils.getDummyRun()
        val runId = dummyRun.runId
        val projectId = dummyRun.projectId
        val request = RunCreateRequest(
          projectVersion = "new-version",
          results = "new-results"
        )

        when(runService.addRun(request, projectId, accessToken.userId)).thenReturn(Future.successful(runId))
        Post(s"/projects/${projectId.value}/runs", request) ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[RunId] shouldEqual runId
        }
      }

      "returns server error if run addition was failed" taggedAs Controller in {
        val projectId = TestRunUtils.getDummyProjectId
        val request = RunCreateRequest(
          projectVersion = "new-version",
          results = "new-results"
        )

        when(runService.addRun(request, projectId, accessToken.userId))
          .thenReturn(Future.failed(new RuntimeException("something went wrong")))

        Post(s"/projects/${projectId.value}/runs", request) ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }
  }
}
