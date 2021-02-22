package cromwell.pipeline.datastorage.dao.mongo

import org.mongodb.scala.Document

trait DocumentEncoder[T] {
  def toDocument(t: T): Document
}

object DocumentEncoder {
  implicit class DocumentEncoderSyntax[T: DocumentEncoder](t: T) {
    def toDocument: Document = implicitly[DocumentEncoder[T]].toDocument(t)
  }
}

trait DocumentDecoder[T] {
  def fromDocument(document: Document): T
}

object DocumentDecoder {
  implicit class DocumentDecoderSyntax(val document: Document) extends AnyVal {
    def fromDocument[T: DocumentDecoder]: T = implicitly[DocumentDecoder[T]].fromDocument(document)
  }
}

trait DocumentCodec[T] extends DocumentEncoder[T] with DocumentDecoder[T]

object DocumentCodec {
  def apply[T](encoder: DocumentEncoder[T], decoder: DocumentDecoder[T]): DocumentCodec[T] =
    new DocumentCodec[T] {
      override def toDocument(t: T): Document = encoder.toDocument(t)
      override def fromDocument(document: Document): T = decoder.fromDocument(document)
    }
}
