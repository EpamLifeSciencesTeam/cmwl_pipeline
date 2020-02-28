package cromwell.pipeline.utils.golden

import cats.Applicative
import cats.instances.list._
import cats.instances.try_._
import cats.syntax.traverse._
import cats.laws._
import play.api.libs.json.{JsError, Json, JsResult, JsSuccess, JsValue}

import scala.util.{Failure, Success, Try}

trait CodecLaws[A] {
  protected def goldenSamples: Try[List[(A, String)]]

  def serialize: A => JsValue

  def deserialize: JsValue => JsResult[A]

  def codecRoundTrip(a: A)(implicit applicative: Applicative[JsResult]): IsEq[JsResult[A]] =
    serialize.andThen(deserialize)(a) <-> applicative.pure(a)

  def encoding: Try[List[IsEq[String]]] = goldenSamples.map {
    _.map {
      case (value, encoded) =>
        Json.stringify(serialize(value)) <-> encoded
    }
  }

  def decoding: Try[List[IsEq[A]]] = goldenSamples.flatMap {
    _.traverse {
      case (value, encoded) =>
        deserialize(Json.parse(encoded)) match {
          case JsSuccess(decoded, _) => Success(decoded <-> value)
          case error: JsError => Failure(error.get)
        }
    }
  }
}


