package cromwell.pipeline

import java.nio.file.{ Files, Paths }

import com.typesafe.config.ConfigFactory
import common.Checked
import common.validation.Validation._
import cromwell.core.path.{ Path, PathBuilderFactory }
import cromwell.languages.LanguageFactory
import cromwell.languages.util.ImportResolver._
import languages.cwl.CwlV1_0LanguageFactory
import languages.wdl.biscayne.WdlBiscayneLanguageFactory
import languages.wdl.draft2.WdlDraft2LanguageFactory
import languages.wdl.draft3.WdlDraft3LanguageFactory
import wom.ResolvedImportRecord
import wom.executable.WomBundle
import wom.expression.NoIoFunctionSet
import wom.graph._
import womtool.validate.Validate

import scala.collection.JavaConverters._
//import scala.reflect.io.Path
import scala.util.Try

object ValidationFromFileContents extends App {

  def getBundle(mainFile: Path): Checked[WomBundle] = getBundleAndFactory(mainFile).map(_._1)

  private def getBundleAndFactory(mainFile: Path): Checked[(WomBundle, LanguageFactory)] = {
    lazy val importResolvers: List[ImportResolver] =
      DirectoryResolver.localFilesystemResolvers(Some(mainFile)) :+ HttpResolver(relativeTo = None)

    readFile(mainFile.toAbsolutePath.pathAsString).flatMap { mainFileContents =>
      val languageFactory =
        List(
          new WdlDraft3LanguageFactory(ConfigFactory.empty()),
          new WdlBiscayneLanguageFactory(ConfigFactory.empty()),
          new CwlV1_0LanguageFactory(ConfigFactory.empty())
        ).find(_.looksParsable(mainFileContents)).getOrElse(new WdlDraft2LanguageFactory(ConfigFactory.empty()))

      val bundle = languageFactory.getWomBundle(mainFileContents, None, "{}", importResolvers, List(languageFactory))
      // Return the pair with the languageFactory
      bundle.map((_, languageFactory))
    }
  }

  private def readFile(filePath: String): Checked[String] = Try(
    Files.readAllLines(Paths.get(filePath)).asScala.mkString(System.lineSeparator())
  ).toChecked

//  private def runAppShowStrings(mainFileContents: String): Unit = {
//    lazy val importResolvers: List[ImportResolver] =
//      DirectoryResolver.localFilesystemResolvers(Some(mainFile)) :+ HttpResolver(relativeTo = None)
//
//    readFile(mainFile.toAbsolutePath.pathAsString).flatMap { mainFileContents =>
//      val languageFactory =
//        List(
//          new WdlDraft3LanguageFactory(ConfigFactory.empty()),
//          new WdlBiscayneLanguageFactory(ConfigFactory.empty()),
//          new CwlV1_0LanguageFactory(ConfigFactory.empty())
//        ).find(_.looksParsable(mainFileContents)).getOrElse(new WdlDraft2LanguageFactory(ConfigFactory.empty()))
//
//      val bundle = languageFactory.getWomBundle(mainFileContents, None, "{}", importResolvers, List(languageFactory))
//      // Return the pair with the languageFactory
//      bundle.map((_, languageFactory))
//    }
//  }

//  private def runAppTestValid(mainFileContentsPath: String): Unit = {
//
//    Validate.validate(v.workflowSource, v.inputs, v.listDependencies)
//
////    lazy val importResolvers: List[ImportResolver] = List()
//
////    lazy val importResolvers: List[ImportResolver] =
////      DirectoryResolver.localFilesystemResolvers(Some(mainFile)) :+ HttpResolver(relativeTo = None)
//
//    readFile(mainFileContentsPath).flatMap { mainFileContents =>
//      val languageFactory =
//        List(
//          new WdlDraft3LanguageFactory(ConfigFactory.empty()),
//          new WdlBiscayneLanguageFactory(ConfigFactory.empty()),
//          new CwlV1_0LanguageFactory(ConfigFactory.empty())
//        ).find(_.looksParsable(mainFileContents)).getOrElse(new WdlDraft2LanguageFactory(ConfigFactory.empty()))
//
//      val bundle = languageFactory.getWomBundle(mainFileContents, None, "{}", importResolvers, List(languageFactory))
//      // Return the pair with the languageFactory
//      bundle.map((_, languageFactory))
//    }
//  }
//
//  runAppTestValid("/home/benderbej/WDL/workflows/hello.wdl");

}
