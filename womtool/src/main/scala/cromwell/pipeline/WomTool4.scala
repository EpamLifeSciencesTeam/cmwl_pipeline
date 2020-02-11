package cromwell.pipeline

import cats.data.NonEmptyList
import com.typesafe.config.ConfigFactory
import cromwell.languages.LanguageFactory
import cromwell.languages.util.ImportResolver.{ DirectoryResolver, HttpResolver, ImportResolver }
import languages.cwl.CwlV1_0LanguageFactory
import languages.wdl.biscayne.WdlBiscayneLanguageFactory
import languages.wdl.draft2.WdlDraft2LanguageFactory
import languages.wdl.draft3.WdlDraft3LanguageFactory
import wom.executable.WomBundle

object WomTool4 extends App {
  val mainFileContents =
    """
      |task hello {
      |  String name
      |
      |  command {
      |    echo 'Hello world!'
      |  }
      |  output {
      |    File response = stdout()
      |  }
      |}
      |
      |workflow test {
      |  call hello
      |}
      |""".stripMargin
  lazy val importResolvers: List[ImportResolver] =
    DirectoryResolver.localFilesystemResolvers(None) :+ HttpResolver(relativeTo = None)
  val languageFactory: LanguageFactory =
    List(
      new WdlDraft3LanguageFactory(ConfigFactory.empty()),
      new WdlBiscayneLanguageFactory(ConfigFactory.empty()),
      new CwlV1_0LanguageFactory(ConfigFactory.empty())
    ).find(_.looksParsable(mainFileContents)).getOrElse(new WdlDraft2LanguageFactory(ConfigFactory.empty()))

  val bundle = languageFactory.getWomBundle(mainFileContents, None, "{}", importResolvers, List(languageFactory))
  println(bundle.map((_, languageFactory)))
}
