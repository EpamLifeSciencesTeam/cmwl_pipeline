package cromwell.pipeline.datastorage.dto

import java.nio.file.{ Path, Paths }

import ProjectFile.pathFormat
import cromwell.pipeline.utils.json.AdtJsonFormatter._
import play.api.libs.json._

sealed trait TypedValue
final case class StringTyped(value: Option[String]) extends TypedValue
final case class FileTyped(value: Option[Path]) extends TypedValue
final case class IntTyped(value: Option[Int]) extends TypedValue
final case class FloatTyped(value: Option[Float]) extends TypedValue
final case class BooleanTyped(value: Option[Boolean]) extends TypedValue

object TypedValue {
  type TypeValuePair = (String, Option[String])

  implicit val typedValueFormat: OFormat[TypedValue] = {
    implicit val stringTypedFormat: OFormat[StringTyped] = Json.format
    implicit val fileTyped: OFormat[FileTyped] = Json.format
    implicit val intTyped: OFormat[IntTyped] = Json.format
    implicit val floatTyped: OFormat[FloatTyped] = Json.format
    implicit val booleanTyped: OFormat[BooleanTyped] = Json.format

    adtFormat("_type")(
      adtCase[StringTyped]("String"),
      adtCase[FileTyped]("File"),
      adtCase[IntTyped]("Int"),
      adtCase[FloatTyped]("Float"),
      adtCase[BooleanTyped]("Boolean")
    )
  }

  def fromPair(pair: TypeValuePair): TypedValue = pair match {
    case ("String", strLike)   => StringTyped(strLike)
    case ("File", pathLike)    => FileTyped(pathLike.map(Paths.get(_)))
    case ("Int", digitLike)    => IntTyped(digitLike.map(_.toInt))
    case ("Float", digitLike)  => FloatTyped(digitLike.map(_.toFloat))
    case ("Boolean", boolLike) => BooleanTyped(boolLike.map(_.toBoolean))
    case (typeVal, _) =>
      throw new RuntimeException(s"$typeVal is not supported")
  }
}
