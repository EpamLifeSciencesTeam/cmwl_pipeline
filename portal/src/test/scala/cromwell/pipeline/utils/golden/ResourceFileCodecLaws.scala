package cromwell.pipeline.utils.golden

import cats.instances.list._
import cats.instances.try_._
import cats.syntax.apply._
import cats.syntax.traverse._
import java.io.File
import java.io.PrintWriter

import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json.{Format, JsResult, JsValue}

import scala.io.{BufferedSource, Source}
import scala.reflect.runtime.universe.TypeTag
import scala.util.{Failure, Success, Try}

abstract class ResourceFileCodecLaws[A](
  name: String,
  resourceRootDir: File,
  resourcePackage: List[String],
  val size: Int = 100,
  count: Int = 1
) extends CodecLaws[A]
    with SampleGeneration[A] {

  protected lazy val goldenObjects: Try[List[(A, String)]] =
    loadGoldenFiles match {
      case Success(value) if value.isEmpty => generateGoldenFiles
      case Failure(_)                      => generateGoldenFiles
      case _                               => loadGoldenFiles
    }

  private val rootPath = "/" + resourcePackage.mkString("/") + "/"
  private val resourceDir: File = resourcePackage.foldLeft(resourceRootDir) {
    case (acc, p) => new File(acc, p)
  }
  private val goldenFileNamePattern = s"d^$name-(.{50})\\.json$$".r

  private lazy val loadGoldenFiles = {
    Option(getClass.getResourceAsStream(rootPath)) match {
      case Some(inputStream) =>
        val resource = Source.fromInputStream(inputStream)
        open(resource) { dirSource =>
          val files = dirSource.getLines.flatMap {
            case fileName @ goldenFileNamePattern(seed) => Some((seed, fileName))
            case _                                      => None
          }.toList.traverse[Try, (A, String)] {
            case (seed, name) =>
              val contents = Resources.open(rootPath + name).map { source =>
                val lines = source.getLines.mkString("\n")
                source.close()
                lines
              }
              (getValueFromBase64Seed(seed), contents).tupled
          }

          files.flatMap { values =>
            if (values.isEmpty || values.size == count) files
            else Failure(new IllegalStateException(s"Expected 0 or $count golden files, got ${values.size}"))
          }
        }
      case _ => Try(List[(A, String)]())
    }
  }

  private def open[T](source: BufferedSource)(handler: BufferedSource => T): T = {
    try {
      handler(source)
    } finally {
      source.close()
    }
  }

  private def generateGoldenFiles: Try[List[(A, String)]] =
    generateRandomGoldenSamples(count).traverse {
      case (seed, value, serialized) =>
        Try {
          resourceDir.mkdirs()
          val file = new File(resourceDir, s"$name-${seed.toBase64}.json")

          val writer = new PrintWriter(file)
          writer.print(serialized)
          writer.close()

          (value, serialized.toString)
        }
    }
}

object ResourceFileCodecLaws {
  def apply[A](
    name: String,
    resourceRootDir: File,
    resourcePackage: List[String]
  )(implicit arbitraryA: Arbitrary[A], format: Format[A], typeTag: TypeTag[A]): CodecLaws[A] =
    new ResourceFileCodecLaws[A](name, resourceRootDir, resourcePackage) {
      override protected def goldenSamples: Try[List[(A, String)]] = goldenObjects

      override def serialize: A => JsValue = format.writes

      override def deserialize: JsValue => JsResult[A] = format.reads

      override def gen: Gen[A] = Arbitrary.arbitrary(arbitraryA)
    }

  def apply[A](implicit arbitraryA: Arbitrary[A], format: Format[A], typeTag: TypeTag[A]): CodecLaws[A] =
    apply[A](Resources.inferClassName[A], Resources.inferRootDir, Resources.inferPackage[A])
}
