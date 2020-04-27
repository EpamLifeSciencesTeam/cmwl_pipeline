package cromwell.pipeline.service

import java.nio.charset.Charset

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest, HttpResponse }
import akka.http.scaladsl.{ Http, HttpExt }
import akka.stream.ActorMaterializer
import akka.util.ByteString

import scala.concurrent.{ ExecutionContext, Future }

class AkkaHttpClient extends HttpClient {
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val materializer: ActorMaterializer = ActorMaterializer()

  def http: HttpExt = Http()

  override def get(url: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map())(
    implicit ec: ExecutionContext
  ): Future[Response] = {
    val futureResponse: Future[HttpResponse] = http.singleRequest(
      HttpRequest(method = HttpMethods.GET, uri = queryBuilder(url, params)).withHeaders(parseHeaders(headers))
    )
    responsify(futureResponse)
  }

  override def post(
    url: String,
    params: Map[String, String] = Map(),
    headers: Map[String, String] = Map(),
    payload: String
  )(implicit ec: ExecutionContext): Future[Response] = {
    val futureResponse: Future[HttpResponse] = http.singleRequest(
      HttpRequest(method = HttpMethods.POST, uri = queryBuilder(url, params))
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
    val futureResponse: Future[HttpResponse] = http.singleRequest(
      HttpRequest(method = HttpMethods.PUT, uri = queryBuilder(url, params))
        .withEntity(payload)
        .withHeaders(parseHeaders(headers))
    )
    responsify(futureResponse)
  }

  private def responsify(futureResponse: Future[HttpResponse])(implicit ec: ExecutionContext): Future[Response] =
    futureResponse.flatMap { response: HttpResponse =>
      response.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map { body =>
        val responseBody = body.decodeString(Charset.defaultCharset())
        Response(
          response.status.intValue(),
          responseBody,
          response.headers.foldLeft(Map[String, List[String]]()) {
            case (acc, header) =>
              acc + (header.name() -> (header.value() :: acc.getOrElse(header.name(), Nil)))
          }
        )
      }
    }

  private def parseHeaders(headers: Map[String, String]): List[RawHeader] =
    headers.foldLeft(List.empty[RawHeader]) {
      case (acc, (param, value)) => acc ++ List(RawHeader(param, value))
    }

  private def queryBuilder(url: String, params: Map[String, String]): String = {
    val confirmParams = params.map { case (param, value) => s"$param=$value" }
    confirmParams match {
      case p :: ps => url + "?" + confirmParams.mkString("&")
      case _       => url
    }
  }
}
