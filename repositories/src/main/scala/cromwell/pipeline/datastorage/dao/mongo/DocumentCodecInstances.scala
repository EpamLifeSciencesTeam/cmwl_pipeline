package cromwell.pipeline.datastorage.dao.mongo

import cromwell.pipeline.datastorage.dto.ProjectConfiguration
import org.mongodb.scala.bson.collection.immutable.Document
import play.api.libs.json.{ Format, Json, Reads, Writes }

object DocumentCodecInstances {

  private def playJsonDocumentEncoder[T: Writes]: DocumentEncoder[T] =
    (t: T) => Document(Json.stringify(Json.toJson(t)))

  private def playJsonDocumentDecoder[T: Reads]: DocumentDecoder[T] =
    (document: Document) => Json.parse(document.toJson()).as[T]

  private def playJsonDocumentCodec[T: Format]: DocumentCodec[T] =
    DocumentCodec(playJsonDocumentEncoder, playJsonDocumentDecoder)

  implicit val projectConfigurationDocumentCodec: DocumentCodec[ProjectConfiguration] = playJsonDocumentCodec

}
