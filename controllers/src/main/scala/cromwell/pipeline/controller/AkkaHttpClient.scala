package cromwell.pipeline.controller

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest, HttpResponse, Uri }
import akka.stream.ActorMaterializer
import cromwell.pipeline.service.{ HttpClient, Response }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

class AkkaHttpClient(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) extends HttpClient {
  private val expirationTime: FiniteDuration = 300.millis
  override def get(url: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map())(
    implicit ec: ExecutionContext
  ): Future[Response] = {
    val futureResponse: Future[HttpResponse] = Http().singleRequest(
      HttpRequest(method = HttpMethods.GET, uri = Uri(url).withQuery(Query(params))).withHeaders(parseHeaders(headers))
    )
    responsify(futureResponse)
  }

  override def delete(url: String, headers: Map[String, String] = Map())(
    implicit ec: ExecutionContext
  ): Future[Response] = {
    val futureResponse: Future[HttpResponse] = Http().singleRequest(
      HttpRequest(method = HttpMethods.DELETE, uri = Uri(url)).withHeaders(parseHeaders(headers))
    )
    responsify(futureResponse)
  }

  private def responsify(futureResponse: Future[HttpResponse])(implicit ec: ExecutionContext): Future[Response] =
    futureResponse.flatMap { response =>
      response.entity.toStrict(expirationTime).map { body =>
        Response(
          response.status.intValue(),
          body.data.utf8String,
          response.headers.foldLeft(Map[String, List[String]]()) {
            case (acc, header) =>
              acc + (header.name() -> (header.value() :: acc.getOrElse(header.name(), Nil)))
          }
        )
      }
    }

  private def parseHeaders(headers: Map[String, String]): List[RawHeader] =
    headers.map { case (param, value) => RawHeader(param, value) }.toList

  override def post(
    url: String,
    params: Map[String, String] = Map(),
    headers: Map[String, String] = Map(),
    payload: String
  )(implicit ec: ExecutionContext): Future[Response] = {
    val futureResponse: Future[HttpResponse] = Http().singleRequest(
      HttpRequest(method = HttpMethods.POST, uri = Uri(url).withQuery(Query(params)))
        .withEntity(payload)
        .withHeaders(parseHeaders(headers))
    )
    responsify(futureResponse)
  }

  override def put(
    url: String,
    params: Map[String, String] = Map(),
    headers: Map[String, String] = Map(),
    payload: String
  )(implicit ec: ExecutionContext): Future[Response] = {
    val futureResponse: Future[HttpResponse] = Http().singleRequest(
      HttpRequest(method = HttpMethods.PUT, uri = Uri(url).withQuery(Query(params)))
        .withEntity(payload)
        .withHeaders(parseHeaders(headers))
    )
    responsify(futureResponse)
  }
}
