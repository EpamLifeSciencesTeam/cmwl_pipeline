package cromwell.pipeline.utils.json

import cromwell.pipeline.utils.json.AdtJsonFormatter._
import play.api.libs.json._

import scala.reflect.{ ClassTag, classTag }
import scala.util.control.NoStackTrace

trait AdtJsonFormatter {

  sealed trait AdtCase[T] {
    def typeFieldValue: String
  }
  sealed trait AdtCaseWrites[T] extends AdtCase[T] {
    def canWrite(o: Any): Boolean
    def writes(o: Any): JsObject
  }
  sealed trait AdtCaseReads[T] extends AdtCase[T] {
    def reads(o: JsObject): JsResult[T]
  }
  sealed trait AdtCaseFormat[T] extends AdtCaseWrites[T] with AdtCaseReads[T]

  object AdtCaseFormat {
    def apply[T: OFormat: ClassTag](_typeFieldValue: String): AdtCaseFormat[T] =
      new AdtCaseFormat[T] {
        override def reads(o: JsObject): JsResult[T] = implicitly[Reads[T]].reads(o)

        override def canWrite(o: Any): Boolean = o match {
          case _: T => true
          case _    => false
        }
        override def writes(o: Any): JsObject = o match {
          case t: T => implicitly[OWrites[T]].writes(t)
          case other =>
            val expectedType = classTag[T].runtimeClass.getSimpleName
            val unexpectedType = other.getClass.getSimpleName
            throw new AdtJsonFormatterException(s"Expected type [$expectedType], but got [$unexpectedType]")
        }

        override val typeFieldValue: String = _typeFieldValue
      }
  }

  def adtFormat[T](typeFieldName: String)(cases: AdtCaseFormat[_ <: T]*): OFormat[T] =
    OFormat(adtReads[T](typeFieldName)(cases: _*), adtWrites[T](typeFieldName)(cases: _*))

  def adtReads[T](typeFieldName: String)(cases: AdtCaseReads[_ <: T]*): Reads[T] = Reads { jsValue =>
    def readObject(obj: JsObject): JsResult[T] =
      obj.value.get(typeFieldName) match {
        case Some(jsValue) => jsValue.validate[String].flatMap(readTypedObject(obj, _))
        case None          => JsError(s"Couldn't find `typeFieldName` hint in object [$obj]")
      }

    def readTypedObject(obj: JsObject, typeFieldValue: String): JsResult[T] =
      cases.find(_.typeFieldValue == typeFieldValue) match {
        case Some(reads) => reads.reads(obj - typeFieldName)
        case None        => JsError(s"Couldn't find Reads for object [$obj]")
      }

    jsValue match {
      case obj: JsObject => readObject(obj)
      case other         => JsError(s"Expected JsObject but got [${other.getClass.getSimpleName}]")
    }
  }

  def adtWrites[T](typeFieldName: String)(cases: AdtCaseWrites[_ <: T]*): OWrites[T] = OWrites { obj: T =>
    cases
      .find(_.canWrite(obj))
      .map { writes =>
        writes.writes(obj) match {
          case JsObject(fields) if fields.contains(typeFieldName) =>
            throw new AdtJsonFormatterException(s"Object [$obj] already contains field [$typeFieldName]")
          case jsObject: JsObject =>
            JsObject((typeFieldName, JsString(writes.typeFieldValue)) +: jsObject.fields)
        }
      }
      .getOrElse(throw new AdtJsonFormatterException(s"Couldn't find writer for object [$obj]"))
  }

  def adtCase[T: OFormat: ClassTag](typeFieldValue: String): AdtCaseFormat[T] = AdtCaseFormat(typeFieldValue)

  def objectFormat[T <: Singleton](obj: T): OFormat[T] =
    OFormat(objectReads(obj), objectWrites)

  private def objectReads[T](obj: T): Reads[T] = Reads {
    case _: JsObject => JsSuccess(obj)
    case other       => JsError(s"Expected JsObject but got [${other.getClass.getSimpleName}]")
  }
  private def objectWrites[T]: OWrites[T] = OWrites(_ => JsObject.empty)

}

object AdtJsonFormatter extends AdtJsonFormatter {
  final class AdtJsonFormatterException(message: String) extends Exception(message: String) with NoStackTrace
}
