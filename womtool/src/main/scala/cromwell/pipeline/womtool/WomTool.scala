package cromwell.pipeline.womtool

import java.nio.file.{ Files, Paths }

import cats.data.NonEmptyList
import com.typesafe.config.ConfigFactory
import common.Checked
import cromwell.core.path.{ DefaultPath, DefaultPathBuilder, Path }
import cromwell.languages.LanguageFactory
import cromwell.languages.util.ImportResolver.ImportResolver
import languages.cwl.CwlV1_0LanguageFactory
import languages.wdl.biscayne.WdlBiscayneLanguageFactory
import languages.wdl.draft2.WdlDraft2LanguageFactory
import languages.wdl.draft3.WdlDraft3LanguageFactory
import wom.executable.WomBundle
import womtool.WomtoolMain.{ BadUsageTermination, SuccessfulTermination, Termination, UnsuccessfulTermination }
import womtool.inputs.Inputs

import scala.util.Try

class WomTool extends WomToolAPI {

  def validate(
    content: String,
    importResolvers: List[ImportResolver]
  ): Either[NonEmptyList[String], (WomBundle, LanguageFactory)] = {

    val languageFactory: LanguageFactory =
      List(
        new WdlDraft3LanguageFactory(ConfigFactory.empty()),
        new WdlBiscayneLanguageFactory(ConfigFactory.empty()),
        new CwlV1_0LanguageFactory(ConfigFactory.empty())
      ).find(_.looksParsable(content)).getOrElse(new WdlDraft2LanguageFactory(ConfigFactory.empty()))

    val bundle: Checked[WomBundle] =
      languageFactory.getWomBundle(content, None, "{}", importResolvers, List(languageFactory))

    def getBundleAndFactory(womBundle: Checked[WomBundle]): Either[NonEmptyList[String], (WomBundle, LanguageFactory)] =
      womBundle match {
        case Right(w) => Right(w, languageFactory)
        case Left(l)  => Left(l)
      }
    getBundleAndFactory(bundle)
  }

  def inputs(content: String): Either[NonEmptyList[String], String] = {

    def createTmpFile(wdlFileContent: String): Try[java.nio.file.Path] =
      Try(Files.createTempFile("tmp", ".wdl"))

    def writeTmpFile(path: java.nio.file.Path): Try[java.nio.file.Path] =
      Try(Files.write(path, content.getBytes()))

    def deleteTmpFile(tempFile: java.nio.file.Path): Unit =
      Files.delete(tempFile)

    def getTryiedPath(tmpPath: java.nio.file.Path): Try[DefaultPath] =
      DefaultPathBuilder.build(tmpPath.toString)

    def getTermination(path: Path): Termination = Inputs.inputsJson(path, false)

    def getEither(term: Termination): Either[NonEmptyList[String], String] = term match {
      case SuccessfulTermination(x)   => new Right(x)
      case UnsuccessfulTermination(x) => new Left(NonEmptyList(x, Nil))
      case BadUsageTermination(x)     => new Left(NonEmptyList(x, Nil))
    }

    val tmpPath: Try[DefaultPath] = for {
      created <- createTmpFile(content)
      written <- writeTmpFile(created)
      tPath <- getTryiedPath(written)
    } yield (tPath)

    val either = getEither(getTermination(tmpPath.get))
    deleteTmpFile(Paths.get(tmpPath.get.pathAsString))
    either
  }
}
