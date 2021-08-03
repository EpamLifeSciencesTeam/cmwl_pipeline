package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cromwell.pipeline.datastorage.dao.utils.{ TestRunUtils, TestUserUtils }
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
        val runRespOption: Option[Run] = Option(dummyRun)

        when(runService.getRunByIdAndUser(runId, accessToken.userId)).thenReturn(Future.successful(runRespOption))

        Get(s"/runs/$runId") ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Option[Run]] shouldEqual runRespOption
        }
      }

      "returns the internal server error if service fails" taggedAs Controller in {
        val runId = TestRunUtils.getDummyRunId
        when(runService.getRunByIdAndUser(runId, accessToken.userId))
          .thenReturn(Future.failed(new RuntimeException("something went wrong")))

        Get(s"/runs/$runId") ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }

      "returns NotFound if the run id is not valid" taggedAs Controller in {
        val invalidRunId = "123"
        Get(s"/runs/$invalidRunId") ~> Route.seal(runController.route(accessToken)) ~> check {
          status shouldBe StatusCodes.NotFound
        }
      }
    }

    "deleteRunById" should {
      "returns run 1 if the entity was deleted" taggedAs Controller in {
        val runId = TestRunUtils.getDummyRunId

        when(runService.deleteRunById(runId, accessToken.userId)).thenReturn(Future.successful(1))

        Delete(s"/runs/$runId") ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
        }
      }
      "returns server error if run delete was failed" taggedAs Controller in {
        val runId = TestRunUtils.getDummyRunId
        when(runService.deleteRunById(runId, accessToken.userId))
          .thenReturn(Future.failed(new RuntimeException("something went wrong")))

        Delete(s"/runs/$runId") ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
      "returns InternalServerError status if run id doesn't match" in {
        val runId = TestRunUtils.getDummyRunId
        when(runService.deleteRunById(runId, accessToken.userId))
          .thenReturn(Future.failed(new RuntimeException("Something wrong.")))

        Delete(s"/runs/$runId") ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }

    "updates run entity" should {
      "returns NoContent status if run was updated" in {
        val runId = TestRunUtils.getDummyRunId
        val dummyRun: Run = TestRunUtils.getDummyRun()
        val request = RunUpdateRequest(
          dummyRun.status,
          dummyRun.timeStart,
          dummyRun.timeEnd,
          dummyRun.results,
          dummyRun.cmwlWorkflowId
        )
        when(runService.updateRun(runId, request, accessToken.userId)).thenReturn(Future.successful(1))

        Put(s"/runs/$runId", request) ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NoContent
        }
      }

      "returns NoContent status if run entity was updated without time end" in {
        val runId = TestRunUtils.getDummyRunId
        val dummyRun: Run = TestRunUtils.getDummyRun()
        val request = RunUpdateRequest(
          dummyRun.status,
          dummyRun.timeStart,
          None,
          dummyRun.results,
          dummyRun.cmwlWorkflowId
        )
        when(runService.updateRun(runId, request, accessToken.userId)).thenReturn(Future.successful(1))

        Put(s"/runs/$runId", request) ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.NoContent
        }
      }

      "returns InternalServerError status if run id doesn't match" in {
        val runId = TestRunUtils.getDummyRunId
        val dummyRun: Run = TestRunUtils.getDummyRun()
        val request = RunUpdateRequest(
          dummyRun.status,
          dummyRun.timeStart,
          None,
          dummyRun.results,
          dummyRun.cmwlWorkflowId
        )

        when(runService.updateRun(runId, request, accessToken.userId))
          .thenReturn(Future.failed(new RuntimeException("something went wrong")))

        Put(s"/runs/$runId", request) ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }

    "adds run entity" should {

      "returns the run id" taggedAs Controller in {
        val runId = TestRunUtils.getDummyRunId
        val request = RunCreateRequest(
          projectId = TestRunUtils.getDummyProjectId,
          projectVersion = "new-version",
          results = "new-results"
        )

        when(runService.addRun(request, accessToken.userId)).thenReturn(Future.successful(runId))
        Post("/runs", request) ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[RunId] shouldEqual runId
        }
      }

      "returns server error if run addition was failed" taggedAs Controller in {
        val request = RunCreateRequest(
          projectId = TestRunUtils.getDummyProjectId,
          projectVersion = "new-version",
          results = "new-results"
        )
        when(runService.addRun(request, accessToken.userId))
          .thenReturn(Future.failed(new RuntimeException("something went wrong")))

        Post("/runs", request) ~> runController.route(accessToken) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }
  }
}
