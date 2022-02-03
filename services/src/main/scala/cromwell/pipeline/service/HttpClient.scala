package cromwell.pipeline.service

/**
 * This trait force implementations to provide REST operations using PlayJson, based on Reads & Writes
 * @get,
 * @post,
 * @put
 * @delete methods have Response generic type, which is
 * @ResponseBoby type of its Response
 *
 * Both ResponseBody (B) and Payload (P) are parametrized
 * To use this client you have to have implicit Reads or Writes for your entities in your DTO
 *
 * @version 1.0
 */
import play.api.libs.json.{ Reads, Writes }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Trait with standard REST operations, with parametrized methods, where B is ResponseBody type and P is Payload type
 * (bf reads as 'body formatter')
 */
trait HttpClient {

  def get[B](url: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map())(
    implicit ec: ExecutionContext,
    f: Reads[B]
  ): Future[Response[B]]
  def post[B, P](url: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map(), payload: P)(
    implicit ec: ExecutionContext,
    bf: Reads[B],
    pf: Writes[P]
  ): Future[Response[B]]
  def put[B, P](url: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map(), payload: P)(
    implicit ec: ExecutionContext,
    bf: Reads[B],
    pf: Writes[P]
  ): Future[Response[B]]
  def delete[B, P](url: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map(), payload: P)(
    implicit ec: ExecutionContext,
    bf: Reads[B],
    pf: Writes[P]
  ): Future[Response[B]]
}

/**
 * Contains response data with unmarshalled Json
 *
 * @param status http status code
 * @param body wrapper class contains unmarshalled object of type
 * @param headers Map of http headers
 * @tparam B type of object you unmarshall to
 */
case class Response[B](status: Int, body: ResponseBody[B], headers: Map[String, Seq[String]])

/**
 * Wrapper trait, contains response body of defined type
 *
 * @tparam B Response type
 */
sealed trait ResponseBody[B]

/**
 * Positive projection of [[ResponseBody]],
 * describe a wrapper object for successfully unmarshalled(parsed from json and validated) object instance
 *
 * @param bodyObject
 * @tparam B Type which response contains
 */
final case class SuccessResponseBody[B](bodyObject: B) extends ResponseBody[B]

/**
 * Negative projection of [cromwell.pipeline.service.ResponseBody],
 * with rejection info, such as unsuccessful status code or json validation error
 *
 * @param error
 * @tparam B Type which response contains
 */
final case class FailureResponseBody[B](error: String) extends ResponseBody[B]
