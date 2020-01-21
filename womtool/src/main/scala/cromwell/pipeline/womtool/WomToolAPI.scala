package cromwell.pipeline.womtool

import cats.data.NonEmptyList
import wom.executable.WomBundle

trait WomToolAPI {

  def validate(content: String): Either[NonEmptyList[String], WomBundle]

  def inputs(content: String): Either[NonEmptyList[String], String]
}
