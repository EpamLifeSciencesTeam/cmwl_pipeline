package cromwell.pipeline

import java.nio.file.{ Files, Paths }

import com.typesafe.config.ConfigFactory
import common.Checked
import common.util.VersionUtil
import cromwell.core.path.Path
import cromwell.languages.LanguageFactory
import cromwell.languages.util.ImportResolver.{ DirectoryResolver, HttpResolver, ImportResolver }
import languages.cwl.CwlV1_0LanguageFactory
import languages.wdl.biscayne.WdlBiscayneLanguageFactory
import languages.wdl.draft2.WdlDraft2LanguageFactory
import languages.wdl.draft3.WdlDraft3LanguageFactory
import wom.ResolvedImportRecord
import wom.executable.WomBundle
import wom.expression.NoIoFunctionSet
import wom.graph.Graph
import womtool.WomtoolMain.{
  BadUsageTermination,
  SuccessfulTermination,
  Termination,
  UnsuccessfulTermination,
  args,
  dispatchCommand,
  graph,
  highlight,
  parse,
  upgrade,
  womGraph
}
import womtool.cmdline.WomtoolCommand.{ Graph, Highlight, Inputs, Parse, Upgrade, Validate, WomGraph }
import womtool.cmdline.{
  HighlightCommandLine,
  InputsCommandLine,
  ParseCommandLine,
  PartialWomtoolCommandLineArguments,
  ValidateCommandLine,
  ValidatedWomtoolCommandLine,
  WomtoolCommandLineParser,
  WomtoolGraphCommandLine,
  WomtoolWdlUpgradeCommandLine,
  WomtoolWomGraphCommandLine
}
//import womtool.input.WomGraphMaker
import womtool.inputs.Inputs

import scala.util.Try
//import womtool.validate.Validate
//*****************************************WomGraphMaker

import java.nio.file.{ Files, Paths }

import com.typesafe.config.ConfigFactory
import common.Checked
import common.validation.Validation._
import cromwell.core.path.Path
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

import scala.collection.JavaConverters._
import scala.util.Try

object WomTool2 extends App {

  println(args(0))
  println(args(1))

  val termination = runWomtool(args)

  def runWomtool(cmdLineArgs: Seq[String]): Termination = {
    val parsedArgs: Option[ValidatedWomtoolCommandLine] = WomtoolCommandLineParser.instance
      .parse(cmdLineArgs, PartialWomtoolCommandLineArguments())
      .flatMap(WomtoolCommandLineParser.validateCommandLine)

    println("parsedArgs.map(x => println(x))=" + parsedArgs.map(x => x))
//    println("parsedArgs.map(x => println(x))=" + parsedArgs.map(x => println(x)))

    parsedArgs match {
      case Some(pa) => {
        println("args=" + args.toString)
        println("pa=" + pa.toString + " parsedArgs=" + parsedArgs.toString)
        println("parsedArgs.getClass()=" + pa.getClass)
        dispatchCommand(pa)
      };
      case None => BadUsageTermination(WomtoolCommandLineParser.instance.usage)
    }
  }

  def dispatchCommand(commandLineArgs: ValidatedWomtoolCommandLine): Termination =
    commandLineArgs match { //cromwell.core.path.DefaultPath
//  println("parsedArgs.getClass()=" + pa.getClass)
      case v: ValidateCommandLine => {
        println(
          "v.inputs=" + v.inputs + " v.workflowSource=" + v.workflowSource + " v.listDependencies=" + v.listDependencies
        )
        println("v.workflowSource.getClass()=" + v.workflowSource.getClass) //cromwell.core.path.DefaultPath
        validate(v.workflowSource, v.inputs, v.listDependencies)
      }
//    case p: ParseCommandLine             => parse(p.workflowSource.pathAsString)
//    case h: HighlightCommandLine         => highlight(h.workflowSource.pathAsString, h.highlightMode)
//    case i: InputsCommandLine            => Inputs.inputsJson(i.workflowSource, i.showOptionals)
//    case g: WomtoolGraphCommandLine      => graph(g.workflowSource)
//    case g: WomtoolWomGraphCommandLine   => womGraph(g.workflowSource)
//    case u: WomtoolWdlUpgradeCommandLine => upgrade(u.workflowSource.pathAsString)
      case _ => BadUsageTermination(WomtoolCommandLineParser.instance.usage)
    }

  def validate(main: Path, inputs: Option[Path], listDependencies: Boolean): Termination = {
    println("InputsNotDefined main=" + main)
//    println("inputs=" + inputs + " main=" + main)

    def workflowDependenciesMsg(workflowResolvedImports: Set[ResolvedImportRecord]) = {
      val msgPrefix = "\nList of Workflow dependencies is:\n"
      val dependenciesList =
        if (workflowResolvedImports.nonEmpty) workflowResolvedImports.map(_.importPath).mkString("\n") else "None"

      msgPrefix + dependenciesList
    }

    def validationSuccessMsg(workflowResolvedImports: Set[ResolvedImportRecord]): String = {
      val successMsg = "Success!"
      val dependenciesMsg = if (listDependencies) workflowDependenciesMsg(workflowResolvedImports) else ""
      successMsg + dependenciesMsg
    }

    if (inputs.isDefined) {
      println("InputsDefined")
      WomGraphMaker.fromFiles(main, inputs) match {
        case Right(v)     => SuccessfulTermination(validationSuccessMsg(v.resolvedImportRecords))
        case Left(errors) => UnsuccessfulTermination(errors.toList.mkString(System.lineSeparator))
      }
    } else {
//      println("main.getClass=" + main.getClass)
//      println("InputsNotDefined main=" + main)
      WomGraphMaker.getBundle(main) match {
        case Right(b)     => println("succ"); SuccessfulTermination(validationSuccessMsg(b.resolvedImportRecords))
        case Left(errors) => println("unsucc"); UnsuccessfulTermination(errors.toList.mkString(System.lineSeparator))
      }
    }
  }

  object WomtoolCommandLineParser {

    lazy val womtoolVersion = VersionUtil.getVersion("womtool")

    lazy val instance: scopt.OptionParser[PartialWomtoolCommandLineArguments] = new WomtoolCommandLineParser()

    def validateCommandLine(args: PartialWomtoolCommandLineArguments): Option[ValidatedWomtoolCommandLine] =
      args match {
        case PartialWomtoolCommandLineArguments(Some(Validate), Some(mainFile), inputs, None, None, listDependencies) =>
          Option(ValidateCommandLine(mainFile, inputs, listDependencies.getOrElse(false)))
        case _ => None
      }
  }

  object WomGraphMaker {

    def getBundle(mainFile: Path): Checked[WomBundle] =
      getBundleAndFactory(mainFile).map(_._1)

    private def getBundleAndFactory(
      mainFile: Path
    ): Checked[(WomBundle, LanguageFactory)] = {
      lazy val importResolvers: List[ImportResolver] =
        DirectoryResolver.localFilesystemResolvers(Some(mainFile)) :+ HttpResolver(
          relativeTo = None
        )

      readFile(mainFile.toAbsolutePath.pathAsString).flatMap { mainFileContents =>
        val languageFactory =
          List(
            new WdlDraft3LanguageFactory(ConfigFactory.empty()),
            new WdlBiscayneLanguageFactory(ConfigFactory.empty()),
            new CwlV1_0LanguageFactory(ConfigFactory.empty())
          ).find(_.looksParsable(mainFileContents)).getOrElse(new WdlDraft2LanguageFactory(ConfigFactory.empty()))

        val bundle = languageFactory.getWomBundle(
          mainFileContents,
          None,
          "{}",
          importResolvers,
          List(languageFactory)
        )
        // Return the pair with the languageFactory
        bundle.map((_, languageFactory))
      }
    }

    def fromFiles(mainFile: Path, inputs: Option[Path]): Checked[WomGraphWithResolvedImports] =
      getBundleAndFactory(mainFile).flatMap {
        case (womBundle, languageFactory) =>
          inputs match {
            case None =>
              for {
                executableCallable <- womBundle.toExecutableCallable
              } yield WomGraphWithResolvedImports(
                executableCallable.graph,
                womBundle.resolvedImportRecords
              )
            case Some(inputsFile) =>
              for {
                inputsContents <- readFile(inputsFile.toAbsolutePath.pathAsString)
                validatedWomNamespace <- languageFactory.createExecutable(
                  womBundle,
                  inputsContents,
                  NoIoFunctionSet
                )
              } yield WomGraphWithResolvedImports(
                validatedWomNamespace.executable.graph,
                womBundle.resolvedImportRecords
              )
          }
      }

    private def readFile(filePath: String): Checked[String] =
      Try(
        Files.readAllLines(Paths.get(filePath)).asScala.mkString(System.lineSeparator())
      ).toChecked

  }

  case class WomGraphWithResolvedImports(
    graph: Graph,
    resolvedImportRecords: Set[ResolvedImportRecord]
  )

}
