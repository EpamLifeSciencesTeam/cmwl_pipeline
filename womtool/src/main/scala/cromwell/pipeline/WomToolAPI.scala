package cromwell.pipeline

import cats.data.NonEmptyList
import cromwell.languages.LanguageFactory
import io.circe.Json
import wom.executable.WomBundle

trait WomToolAPI {

  def validate(content: String, importResolver: ???): Either[NonEmptyList[String], (WomBundle, LanguageFactory)]

  def inputs(contents: String, importResolver: ???)

//  def generateJsonFromParams(params: Seq[String]): Json
}
