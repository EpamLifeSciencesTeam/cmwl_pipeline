package cromwell.pipeline.controller

/**
 * A class to provide REST operations using PlayJson
 * @get,
 * @post,
 * @put methods have Response generic type, which is
 * @ResponseBoby type of its Response
 *
 * Both ResponseBody (B) and Payload (P) are parametrized
 * To use this client you have to have implicit Reads or Writes for your entities in your DTO
 *
 * Response processed in to steps:
 * 1) firstly it is filtered by Akka success/failure HttpResponse type using response.status.isSuccess() in @responsify method
 * 2) secondly it is filtered if not valid in @validateResponse method
 * as a result if you have both success statusCode and it is validated successfully, you will get [[cromwell.pipeline.service.SuccessResponseBody]],
 * otherwise [[cromwell.pipeline.service.FailureResponseBody]]
 *
 *
 * @version 1.0
 */
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal
import cromwell.pipeline.service.{ FailureResponseBody, HttpClient, Response, SuccessResponseBody }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import play.api.libs.json.{ Json, Reads, Writes }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

/**
 * @constructor Create a new client with a `actorSystem` and `materializer`.
 * @param actorSystem
 */
class AkkaHttpClient(implicit actorSystem: ActorSystem) extends HttpClient {
  private val expirationTime: FiniteDuration = 300.millis

  override def get[B](url: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map())(
    implicit ec: ExecutionContext,
    f: Reads[B]
  ): Future[Response[B]] = {
    val futureResponse: Future[HttpResponse] = Http().singleRequest(
      HttpRequest(method = HttpMethods.GET, uri = Uri(url).withQuery(Query(params))).withHeaders(parseHeaders(headers))
    )
    responsify(futureResponse)
  }

  override def post[B, P](
    url: String,
    params: Map[String, String] = Map(),
    headers: Map[String, String] = Map(),
    payload: P
  )(implicit ec: ExecutionContext, bf: Reads[B], pf: Writes[P]): Future[Response[B]] = {
    val futureResponse: Future[HttpResponse] = Http().singleRequest(
      HttpRequest(method = HttpMethods.POST, uri = Uri(url).withQuery(Query(params)))
        .withEntity(HttpEntity(ContentTypes.`application/json`, Json.stringify(Json.toJson(payload))))
        .withHeaders(parseHeaders(headers))
    )
    responsify[B](futureResponse)
  }

  override def put[B, P](
    url: String,
    params: Map[String, String] = Map(),
    headers: Map[String, String] = Map(),
    payload: P
  )(implicit ec: ExecutionContext, bf: Reads[B], pf: Writes[P]): Future[Response[B]] = {
    val futureResponse: Future[HttpResponse] = Http().singleRequest(
      HttpRequest(method = HttpMethods.PUT, uri = Uri(url).withQuery(Query(params)))
        .withEntity(HttpEntity(ContentTypes.`application/json`, Json.stringify(Json.toJson(payload))))
        .withHeaders(parseHeaders(headers))
    )
    responsify(futureResponse)
  }

  /**
   * Filters unsuccessful responses
   *
   * @return FailureResponseBody if [[akka.http.scaladsl.model.StatusCode]] is not succeed, go to validation otherwise
   */
  private def responsify[B](
    futureResponse: Future[HttpResponse]
  )(implicit ec: ExecutionContext, f: Reads[B]): Future[Response[B]] =
    futureResponse.flatMap { response =>
      if (response.status.isSuccess()) {
        validateResponse(response)
      } else {
        failureResponse(response)
      }
    }

  /**
   * Parse and validate response body
   *
   * @return SuccessResponseBody(value) if body json is valid or FailureResponseBody otherwise
   */
  private def validateResponse[B](
    response: HttpResponse
  )(implicit ec: ExecutionContext, f: Reads[B]): Future[Response[B]] =
    Unmarshal(response.entity).to[B].transform {
      case Success(body) =>
        Try(
          Response[B](
            response.status.intValue(),
            SuccessResponseBody(body),
            parseHeaders(response)
          )
        )
      case Failure(exception) =>
        Try(
          Response[B](
            response.status.intValue(),
            FailureResponseBody(s"Could not parse and validate response. (errors: ${exception.getMessage})"),
            parseHeaders(response)
          )
        )
    }

  private def failureResponse[B](response: HttpResponse)(implicit ec: ExecutionContext): Future[Response[B]] =
    response.entity.toStrict(expirationTime).map { body =>
      Response[B](
        response.status.intValue(),
        FailureResponseBody(body.toString()),
        parseHeaders(response)
      )
    }

  private def parseHeaders(response: HttpResponse): Map[String, Seq[String]] =
    response.headers.foldLeft(Map[String, List[String]]()) {
      case (acc, header) =>
        acc + (header.name() -> (header.value() :: acc.getOrElse(header.name(), Nil)))
    }

  private def parseHeaders(headers: Map[String, String]): List[RawHeader] =
    headers.map { case (param, value) => RawHeader(param, value) }.toList
}
