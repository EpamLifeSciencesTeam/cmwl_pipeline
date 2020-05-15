package cromwell.pipeline.womtool

import cats.data.NonEmptyList
import cromwell.pipeline.datastorage.dto.{ FileParameter, ProjectFileContent }
import wom.executable.WomBundle

trait WomToolAPI {

  def validate(content: String): Either[NonEmptyList[String], WomBundle]

  def stringInputs(content: String): Either[NonEmptyList[String], String]

  def inputsToList(content: String): Either[NonEmptyList[String], List[FileParameter]]
}
