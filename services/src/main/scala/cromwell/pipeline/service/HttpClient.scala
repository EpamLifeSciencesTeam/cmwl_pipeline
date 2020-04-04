package cromwell.pipeline.service

import scala.concurrent.{ ExecutionContext, Future }

trait HttpClient {
  def get(url: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map())(
    implicit ec: ExecutionContext
  ): Future[Response]

  def post(url: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map(), payload: String)(
    implicit ec: ExecutionContext
  ): Future[Response]

  def put(url: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map(), payload: String)(
    implicit ec: ExecutionContext
  ): Future[Response]
}

case class Response(status: Int, body: String, headers: Map[String, Seq[String]])
