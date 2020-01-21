package cromwell.pipeline.womtool

import cats.data.NonEmptyList
import cromwell.languages.LanguageFactory
import cromwell.languages.util.ImportResolver.ImportResolver
import wom.executable.WomBundle

trait WomToolAPI {

  def validate(
    content: String,
    importResolvers: List[ImportResolver]
  ): Either[NonEmptyList[String], (WomBundle, LanguageFactory)]

  def inputs(content: String): Either[NonEmptyList[String], String]
}
