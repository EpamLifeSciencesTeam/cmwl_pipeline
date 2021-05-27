package cromwell.pipeline.utils.configs

import play.api.libs.json.Writes

import scala.language.implicitConversions

private[configs] sealed trait SecretData[T] {
  def underlying: T
}

private[configs] object SecretData {
  private val HIDDEN_INFO = "*********"
  private val HIDDEN_INFO_CA = HIDDEN_INFO.toCharArray

  type ToSecretData[T] = T => SecretData[T]

  private def apply[T](t: T): SecretData[T] = new SecretData[T] {
    override def underlying: T = t
  }

  implicit def secretDataWrites[T: Writes]: Writes[SecretData[T]] =
    implicitly[Writes[T]].contramap(_.underlying)

  implicit def toStringSecretDataInstance: ToSecretData[String] =
    str => SecretData(if (str.nonEmpty) HIDDEN_INFO else str)

  implicit def toCharArraySecretDataInstance: ToSecretData[Array[Char]] =
    arr => SecretData(if (arr.nonEmpty) HIDDEN_INFO_CA else arr)

  implicit def toMapSecretDataInstance[K, V: ToSecretData]: ToSecretData[Map[K, V]] =
    map => SecretData(map.mapValues(toSecretData(_).underlying))

  implicit def toSecretData[T: ToSecretData](t: T): SecretData[T] = implicitly[ToSecretData[T]].apply(t)
}
