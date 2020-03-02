package cromwell.pipeline.utils.golden

import java.io.File
import scala.reflect.runtime.universe.TypeTag
import scala.io.Source
import scala.util.Try

object Resources {

  /**
   * Guess the resources directory in the test directory of the project.
   */
  lazy val inferRootDir: File = {
    var current = new File(getClass.getResource("/").toURI)

    while (current.getName != "target" && current.ne(null)) {
      current = current.getParentFile
    }

    val resourceDir = new File(new File(new File(current.getParentFile, "src"), "test"), "resources")

    resourceDir.mkdirs()
    resourceDir
  }

  def inferPackage[A](implicit A: TypeTag[A]): List[String] =
    A.tpe.typeSymbol.fullName.split('.').init.toList

  def inferClassName[A](implicit A: TypeTag[A]): String =
    A.tpe.typeSymbol.name.decodedName.toString

  def open(path: String): Try[Source] = Try(
    Source.fromInputStream(getClass.getResourceAsStream(path))
  )
}
